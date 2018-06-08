package org.folio.edge.core;

import static org.folio.edge.core.Constants.PARAM_API_KEY;
import static org.folio.edge.core.Constants.SYS_LOG_LEVEL;
import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_PORT;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.SYS_SECURE_STORE_PROP_FILE;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;
import static org.folio.edge.core.utils.test.MockOkapi.X_DURATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpHeaders;
import org.apache.log4j.Logger;
import org.folio.edge.core.model.ClientInfo;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.edge.core.utils.OkapiClientFactory;
import org.folio.edge.core.utils.test.MockOkapi;
import org.folio.edge.core.utils.test.TestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@RunWith(VertxUnitRunner.class)
public class EdgeVerticleTest {

  private static final Logger logger = Logger.getLogger(EdgeVerticleTest.class);

  private static final String apiKey = "Z1luMHVGdjNMZl9kaWt1X2Rpa3U=";
  private static final String badApiKey = "ZnMwMDAwMDAwMA==0000";
  private static final String unknownTenantApiKey = "Z1luMHVGdjNMZl9ib2d1c19ib2d1cw==";
  private static final long requestTimeoutMs = 3000L;

  private static Vertx vertx;
  private static MockOkapi mockOkapi;

  @BeforeClass
  public static void setUpOnce(TestContext context) throws Exception {
    int okapiPort = TestUtils.getPort();
    int serverPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(InstitutionalUserHelper.parseApiKey(apiKey).tenantId);

    mockOkapi = spy(new MockOkapi(okapiPort, knownTenants));
    mockOkapi.start(context);

    vertx = Vertx.vertx();

    System.setProperty(SYS_PORT, String.valueOf(serverPort));
    System.setProperty(SYS_OKAPI_URL, "http://localhost:" + okapiPort);
    System.setProperty(SYS_SECURE_STORE_PROP_FILE, "src/main/resources/ephemeral.properties");
    System.setProperty(SYS_LOG_LEVEL, "DEBUG");
    System.setProperty(SYS_REQUEST_TIMEOUT_MS, String.valueOf(requestTimeoutMs));

    final DeploymentOptions opt = new DeploymentOptions();
    vertx.deployVerticle(TestVerticle.class.getName(), opt, context.asyncAssertSuccess());

    RestAssured.baseURI = "http://localhost:" + serverPort;
    RestAssured.port = serverPort;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @AfterClass
  public static void tearDownOnce(TestContext context) {
    logger.info("Shutting down server");
    vertx.close(res -> {
      if (res.failed()) {
        logger.error("Failed to shut down edge-rtac server", res.cause());
        fail(res.cause().getMessage());
      } else {
        logger.info("Successfully shut down edge-rtac server");
      }

      logger.info("Shutting down mock Okapi");
      mockOkapi.close();
    });
  }

  @Test
  public void testAdminHealth(TestContext context) {
    logger.info("=== Test the health check endpoint ===");

    final Response resp = RestAssured
      .get("/admin/health")
      .then()
      .contentType(TEXT_PLAIN)
      .statusCode(200)
      .extract()
      .response();

    assertEquals("\"OK\"", resp.body().asString());
  }

  @Test
  public void testLoginUnknownApiKey(TestContext context) throws Exception {
    logger.info("=== Test request with unknown apiKey (tenant) ===");

    RestAssured
      .get(String.format("/login/and/do/something?apikey=%s", unknownTenantApiKey))
      .then()
      .contentType(TEXT_PLAIN)
      .statusCode(403);
  }

  @Test
  public void testLoginBadApiKey(TestContext context) throws Exception {
    logger.info("=== Test request with malformed apiKey ===");

    RestAssured
      .get(String.format("/login/and/do/something?apikey=%s", badApiKey))
      .then()
      .contentType(TEXT_PLAIN)
      .statusCode(403);
  }

  @Test
  public void testCachedToken(TestContext context) throws Exception {
    logger.info("=== Test the tokens are cached and reused ===");

    int iters = 5;

    for (int i = 0; i < iters; i++) {
      final Response resp = RestAssured
        .get(String.format("/login/and/do/something?apikey=%s", apiKey))
        .then()
        .contentType(TEXT_PLAIN)
        .statusCode(200)
        .extract()
        .response();

      assertEquals("Success", resp.body().asString());
    }

    verify(mockOkapi).loginHandler(any());
  }

  @Test
  public void testRequestTimeout(TestContext context) throws Exception {
    logger.info("=== Test request timeout ===");

    final Response resp = RestAssured
      .with()
      .header(X_DURATION, requestTimeoutMs * 2)
      .get(String.format("/always/login?apikey=%s", apiKey))
      .then()
      .contentType(TEXT_PLAIN)
      .statusCode(408)
      .extract()
      .response();

    assertEquals("Request Timeout", resp.body().asString());
  }

  public static class TestVerticle extends EdgeVerticle {

    @Override
    public Router defineRoutes() {
      OkapiClientFactory ocf = new OkapiClientFactory(vertx, okapiURL, reqTimeoutMs);
      InstitutionalUserHelper iuHelper = new InstitutionalUserHelper(secureStore);

      Router router = Router.router(vertx);
      router.route(HttpMethod.GET, "/admin/health").handler(this::handleHealthCheck);
      router.route(HttpMethod.GET, "/always/login")
        .handler(new GetTokenHandler(iuHelper, ocf, secureStore, false)::handle);
      router.route(HttpMethod.GET, "/login/and/do/something")
        .handler(new GetTokenHandler(iuHelper, ocf, secureStore, true)::handle);
      return router;
    }
  }

  private static class GetTokenHandler {
    public final boolean useCache;
    public final SecureStore secureStore;
    public final InstitutionalUserHelper iuHelper;
    public final OkapiClientFactory ocf;

    public GetTokenHandler(InstitutionalUserHelper iuHelper, OkapiClientFactory ocf, SecureStore secureStore,
        boolean useCache) {
      this.useCache = useCache;
      this.secureStore = secureStore;
      this.iuHelper = iuHelper;
      this.ocf = ocf;
    }

    public void handle(RoutingContext ctx) {
      ClientInfo clientInfo;
      try {
        clientInfo = InstitutionalUserHelper.parseApiKey(ctx.request().getParam(PARAM_API_KEY));

        OkapiClient client = ocf.getOkapiClient(clientInfo.tenantId);
        CompletableFuture<String> tokenFuture = null;
        if (useCache) {
          tokenFuture = iuHelper.getToken(client, clientInfo.clientId, clientInfo.tenantId, clientInfo.username);
        } else {
          String password = secureStore.get(clientInfo.clientId, clientInfo.tenantId, clientInfo.username);
          tokenFuture = ocf.getOkapiClient(clientInfo.tenantId)
            .login(clientInfo.tenantId, password, ctx.request().headers());
        }
        tokenFuture.thenAcceptAsync(token -> {
          if (token == null) {
            ctx.response()
              .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
              .setStatusCode(403)
              .end("Access Denied");
          } else {
            ctx.response()
              .putHeader(X_OKAPI_TOKEN, token)
              .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
              .setStatusCode(200)
              .end("Success");
          }
        }).exceptionally(t -> {
          ctx.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN);

          if (t != null && t.getCause() instanceof TimeoutException) {
            ctx.response()
              .setStatusCode(408)
              .end("Request Timeout");
          } else {
            ctx.fail(t);
          }
          return null;
        });

      } catch (Exception e) {
        ctx.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
          .setStatusCode(403)
          .end("Access Denied");
      }
    }
  }

}
