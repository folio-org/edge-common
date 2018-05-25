package org.folio.edge.core.utils.test;

import static org.awaitility.Awaitility.await;
import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.DEFAULT_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.core.Constants.X_OKAPI_TENANT;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.awaitility.core.ConditionTimeoutException;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class MockOkapi {

  private static final Logger logger = Logger.getLogger(MockOkapi.class);

  /**
   * Request header that tells MockOkapi to wait at least this long before
   * processing a request.
   * 
   * Exists for the purposes of exercising timeouts
   */
  public static final String X_DURATION = "X-Duration";

  public static final String MOCK_TOKEN = "mynameisyonyonsonicomefromwisconsoniworkatalumbermillthereallthepeopleimeetasiwalkdownthestreetaskhowinthehelldidyougethereisaymynameisyonyonsonicomefromwisconson";

  public final int okapiPort;
  protected final Vertx vertx;
  protected final List<String> knownTenants;

  public MockOkapi(int port, List<String> knownTenants) {
    okapiPort = port;
    vertx = Vertx.vertx();
    this.knownTenants = knownTenants == null ? new ArrayList<>() : knownTenants;
  }

  public void close() {
    vertx.close(res -> {
      if (res.failed()) {
        logger.error("Failed to shut down mock OKAPI server", res.cause());
        fail(res.cause().getMessage());
      } else {
        logger.info("Successfully shut down mock OKAPI server");
      }
    });
  }

  protected Router defineRoutes() {
    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());
    router.route().handler(this::durationHandler);
    router.route(HttpMethod.GET, "/_/proxy/health").handler(this::healthCheckHandler);
    router.route(HttpMethod.POST, "/authn/login").handler(this::loginHandler);

    return router;
  }

  public void start(TestContext context) {

    // Setup Mock Okapi...
    HttpServer server = vertx.createHttpServer();

    final Async async = context.async();
    server.requestHandler(defineRoutes()::accept).listen(okapiPort, result -> {
      if (result.failed()) {
        logger.warn(result.cause());
      }
      context.assertTrue(result.succeeded());
      async.complete();
    });
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
      final long end = System.currentTimeMillis() + dur;
      final long max = dur;

      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        logger.info("Waiting until " + new Date(end) + " before coninuting");
        try {
          await().with()
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .atMost(max, TimeUnit.MILLISECONDS)
            .until(() -> System.currentTimeMillis() > end);
        } catch (ConditionTimeoutException e) {
          logger.info("Continuing request handling after waiting " + max + " ms");
        }
      });
      future.thenRun(ctx::next);
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
    JsonObject body = ctx.getBodyAsJson();

    String tenant = ctx.request().getHeader(X_OKAPI_TENANT);

    String contentType = TEXT_PLAIN;
    int status;
    String resp = null;
    if (tenant == null) {
      status = 400;
      resp = "Unable to process request Tenant must be set";
    } else if (body == null || !body.containsKey("username") || !body.containsKey("password")) {
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

    ctx.response()
      .setStatusCode(status)
      .putHeader(HttpHeaders.CONTENT_TYPE, contentType)
      .end(resp);
  }

}
