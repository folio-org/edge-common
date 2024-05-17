package org.folio.edge.core.utils.test;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.DEFAULT_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.core.Constants.X_OKAPI_TENANT;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.vertx.core.json.DecodeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class MockOkapi {

  private static final Logger logger = LogManager.getLogger(MockOkapi.class);

  /**
   * Request header that tells MockOkapi to wait at least this long before
   * processing a request.
   *
   * Exists for the purposes of exercising timeouts
   *
   * @deprecated Use {@link #setDelay(long)} instead. For security an edge module should not
   *     pass any headers from the external client to the back-end module unless strictly needed.
   */
  @Deprecated(since="4.4.0", forRemoval=true)
  public static final String X_DURATION = "X-Duration";
  /**
   * @deprecated Use proper mocking instead. For security an edge module should not
   *     pass any headers from the external client to the back-end module unless strictly needed.
   */
  @Deprecated(since="4.4.0", forRemoval=true)
  public static final String X_ECHO_STATUS = "X-Echo-Status";

  public static final String MOCK_TOKEN = UUID.randomUUID().toString();

  public final int okapiPort;
  protected final Vertx vertx;
  private final boolean hasOwnVertx;
  protected final List<String> knownTenants;
  private HttpServer httpServer;
  private long delay = 0;

  public MockOkapi(Vertx vertx, int port, List<String> knownTenants) {
    okapiPort = port;
    hasOwnVertx = vertx == null;
    this.vertx = hasOwnVertx ? Vertx.vertx() : vertx;
    this.knownTenants = knownTenants == null ? new ArrayList<>() : knownTenants;
  }

  public MockOkapi(int port, List<String> knownTenants) {
    this(null, port, knownTenants);
  }

  /**
   * Close the HTTP server.
   *
   * <p>If the constructor was called with a non-null vertx then there is no
   * need to call this method because vertx.close() automatically closes the server.
   */
  public Future<Void> close() {
    Future<Void> future;
    if (hasOwnVertx) {
      future = vertx.close();
    } else if (httpServer == null) {
      future = Future.succeededFuture();
    } else {
      future = httpServer.close();
    }
    return future
        .onSuccess(x -> logger.info("Successfully shut down mock OKAPI server"))
        .onFailure(e -> logger.error("Failed to shut down mock OKAPI server", e));
 }

  protected Router defineRoutes() {
    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());
    router.route().handler(this::durationHandler);
    router.route(HttpMethod.GET, "/_/proxy/health").handler(this::healthCheckHandler);
    router.route(HttpMethod.POST, "/authn/login").handler(this::loginHandler);
    router.route("/echo").handler(this::echoHandler);

    return router;
  }

  /**
   * Start the server.
   *
   * <p>JUnit 4 example:
   *
   * <p><pre>
   *   &#64;BeforeClass
   *   public static void setUpOnce(TestContext context) {
   *     MockOkapi mockOkapi = new MockOkapi(8090, List.of("diku"));
   *     mockOkapi.start()
   *     .onComplete(context.asyncAssertSuccess());
   *   }
   * </pre>
   *
   * <p>JUnit 5 example:
   *
   * <p><pre>
   *   &#64;BeforeAll
   *   static void setUpOnce(Vertx vertx, VertxTestContext vtc) {
   *     MockOkapi mockOkapi = new MockOkapi(8090, List.of("diku"));
   *     mockOkapi.start()
   *     .onComplete(vtc.succeedingThenComplete());
   *   }
   * </pre>
   */
  public Future<HttpServer> start() {

    // Setup Mock Okapi...
    var options = new HttpServerOptions().setCompressionSupported(true);
    HttpServer server = vertx.createHttpServer(options);
    return server.requestHandler(defineRoutes()).listen(okapiPort)
        .onFailure(e -> logger.warn(e.getMessage(), e))
        .onSuccess(anHttpServer -> httpServer = anHttpServer);
  }

  public Future<HttpServer> start(HttpServerOptions options) {
    HttpServer server = vertx.createHttpServer(options);
    return server.requestHandler(defineRoutes()).listen(okapiPort)
      .onFailure(e -> logger.warn(e.getMessage(), e))
      .onSuccess(anHttpServer -> httpServer = anHttpServer);
  }

  /**
   * Delay for each request that doesn't have an X-Duration header.
   *
   * @param delay milliseconds to wait, 0 for no wait
   */
  public void setDelay(long delay) {
    this.delay = delay;
  }

  public long getDelay() {
    return delay;
  }

  public void durationHandler(RoutingContext ctx) {
    String duration = ctx.request().getHeader(X_DURATION);
    if (duration != null && !duration.isEmpty()) {
      long dur = DEFAULT_REQUEST_TIMEOUT_MS;
      try {
        dur = Long.parseLong(duration);
      } catch (NumberFormatException e) {
        logger.warn("Invalid value specified for " + X_DURATION + " sleeping default request timeout instead");
      }

      logger.info("Waiting for {} ms before continuing", dur);
      vertx.setTimer(dur, x -> ctx.next());
    } else if (delay > 0) {
      logger.info("Waiting for {} ms before continuing", delay);
      vertx.setTimer(delay, x -> ctx.next());
    } else {
      ctx.next();
    }
  }

  public void healthCheckHandler(RoutingContext ctx) {
    ctx.response()
      .setStatusCode(200)
      .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .end("[ ]");
  }

  public void loginHandler(RoutingContext ctx) {

    String tenant = ctx.request().getHeader(X_OKAPI_TENANT);

    String contentType = TEXT_PLAIN;
    int status;
    String resp;
    try {
      JsonObject body = ctx.getBodyAsJson();
      if (tenant == null) {
        status = 400;
        resp = "Unable to process request Tenant must be set";
      } else if (body.getString("username") == null || body.getString("password") == null) {
        status = 400;
        resp = "Json content error";
      } else if (ctx.request().getHeader(HttpHeaders.CONTENT_TYPE) == null ||
          !ctx.request().getHeader(HttpHeaders.CONTENT_TYPE).equals(APPLICATION_JSON)) {
        status = 400;
        resp = String.format("Content-type header must be [\"%s\"]", APPLICATION_JSON);
      } else if (!knownTenants.contains(tenant)) {
        status = 400;
        resp = String.format("no such tenant %s", tenant);
      } else {
        status = 201;
        resp = body.toString();
        contentType = APPLICATION_JSON;
        ctx.response().putHeader(X_OKAPI_TOKEN, MOCK_TOKEN);
      }
    } catch (DecodeException e) {
      status = 400;
      resp = e.getMessage();
    }
    ctx.response()
      .setStatusCode(status)
      .putHeader(HttpHeaders.CONTENT_TYPE, contentType)
      .end(resp);
  }

  public void echoHandler(RoutingContext ctx) {
    String echoStatus = ctx.request().getHeader(X_ECHO_STATUS);

    int status = 200;
    if (echoStatus != null) {
      try {
        status = Integer.parseInt(echoStatus);
      } catch (NumberFormatException e) {
        logger.error("Exception parsing " + X_ECHO_STATUS, e);
      }
    }
    ctx.response().setStatusCode(status);

    ctx.response().headers().setAll(ctx.request().headers());
    String contentType = ctx.request().getHeader(HttpHeaders.CONTENT_TYPE);
    if (contentType != null) {
      ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType);
    }

    String body = ctx.getBodyAsString();
    if (body != null) {
      ctx.response().end(ctx.getBodyAsString());
    } else {
      ctx.response().end();
    }
  }

}
