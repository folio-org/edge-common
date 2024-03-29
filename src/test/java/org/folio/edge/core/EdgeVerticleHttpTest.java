package org.folio.edge.core;

import static org.folio.edge.core.Constants.SYS_LOG_LEVEL;
import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_PORT;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.SYS_SECURE_STORE_PROP_FILE;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.core.utils.test.MockOkapi.X_ECHO_STATUS;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.utils.ApiKeyUtils;
import org.folio.edge.core.utils.OkapiClientFactory;
import org.folio.edge.core.utils.test.MockOkapi;
import org.folio.edge.core.utils.test.TestUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

@RunWith(VertxUnitRunner.class)
public class EdgeVerticleHttpTest {

  private static final Logger logger = LogManager.getLogger(EdgeVerticleHttpTest.class);

  private static final String apiKey = ApiKeyUtils.generateApiKey("gYn0uFv3Lf", "diku", "diku");
  private static final String badApiKey = apiKey + "0000";
  private static final String unknownTenantApiKey = ApiKeyUtils.generateApiKey("gYn0uFv3Lf", "foobarbaz", "userA");
  private static final int requestTimeoutMs = 5000;

  private static Vertx vertx;
  private static MockOkapi mockOkapi;

  @BeforeClass
  public static void setUpOnce(TestContext context) throws Exception {
    int okapiPort = TestUtils.getPort();
    int serverPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(ApiKeyUtils.parseApiKey(apiKey).tenantId);

    mockOkapi = spy(new MockOkapi(okapiPort, knownTenants));
    mockOkapi.start()
    .onComplete(context.asyncAssertSuccess());

    vertx = Vertx.vertx();

    JsonObject jo = new JsonObject()
        .put(SYS_PORT, serverPort)
        .put(SYS_OKAPI_URL, "http://localhost:" + okapiPort)
        .put(SYS_SECURE_STORE_PROP_FILE, "src/main/resources/ephemeral.properties")
        .put(SYS_LOG_LEVEL, "TRACE")
        .put(SYS_REQUEST_TIMEOUT_MS, requestTimeoutMs);

    final DeploymentOptions opt = new DeploymentOptions().setConfig(jo);
    vertx.deployVerticle(TestVerticleHttp.class.getName(), opt, context.asyncAssertSuccess());

    RestAssured.baseURI = "http://localhost:" + serverPort;
    RestAssured.port = serverPort;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @AfterClass
  public static void tearDownOnce(TestContext context) {
    logger.info("Shutting down server");

    vertx.close()
        .onSuccess(x -> logger.info("Successfully shut down edge-common server"))
        .compose(x -> mockOkapi.close())
        .onSuccess(x -> logger.info("Successfully shut down mock Okapi"))
        .onComplete(context.asyncAssertSuccess());
  }

  @Before
  public void before() {
    mockOkapi.setDelay(0);
  }

  @Test
  public void testAdminHealth() {
    logger.info("=== Test the health check endpoint ===");

    RestAssured
        .get("/admin/health")
        .then()
        .contentType(TEXT_PLAIN)
        .statusCode(200)
        .body(is("\"OK\""));
  }

  @Test
  public void testLoginUnknownApiKey() {
    logger.info("=== Test request with unknown apiKey (tenant) ===");

    RestAssured
        .get(String.format("/login/and/do/something?apikey=%s&foo=bar", unknownTenantApiKey))
        .then()
        .contentType(TEXT_PLAIN)
        .statusCode(403);
  }

  @Test
  public void testLoginMissingApiKey() {
    logger.info("=== Test request without specifying an apiKey ===");

    RestAssured
        .get("/login/and/do/something?apikey=&foo=bar")
        .then()
        .contentType(TEXT_PLAIN)
        .statusCode(401);
  }

  @Test
  public void testLoginBadApiKey() {
    logger.info("=== Test request with malformed apiKey ===");

    RestAssured
        .get(String.format("/login/and/do/something?apikey=%s&foo=bar", badApiKey))
        .then()
        .contentType(TEXT_PLAIN)
        .statusCode(401);
  }

  @Test
  public void testExceptionHandler() {
    logger.info("=== Test the exception handler ===");

    RestAssured
        .get("/internal/server/error")
        .then()
        .contentType(TEXT_PLAIN)
        .statusCode(500);
  }

  @Test
  public void testMissingRequired() {
    logger.info("=== Test request w/ missing required parameter ===");

    RestAssured
        .with()
        .contentType(TEXT_PLAIN)
        .body("success")
        .get(String.format("/login/and/do/something?apikey=%s", apiKey))
        .then()
        .contentType(TEXT_PLAIN)
        .statusCode(400)
        .body(is("Missing required parameter: foo"));
  }

  @Test
  public void test404() {
    logger.info("=== Test 404 response ===");

    RestAssured
        .with()
        .contentType(TEXT_PLAIN)
        .header(X_ECHO_STATUS, "404")
        .body("Not Found")
        .get(String.format("/login/and/do/something?apikey=%s&foo=bar", apiKey))
        .then()
        .contentType(TEXT_PLAIN)
        .statusCode(404)
        .body(is("Not Found"));
  }

  @Test
  public void testCachedToken() {
    logger.info("=== Test the tokens are cached and reused ===");

    int iters = 5;

    for (int i = 0; i < iters; i++) {
      RestAssured
          .with()
          .contentType(TEXT_PLAIN)
          .body("success")
          .get(String.format("/login/and/do/something?apikey=%s&foo=bar", apiKey))
          .then()
          .contentType(TEXT_PLAIN)
          .statusCode(200)
          .body(is("success"));
    }

    verify(mockOkapi).loginHandler(any());
  }

  @Test
  public void testRequestTimeout() {
    logger.info("=== Test request timeout ===");

    RestAssured
        .get(String.format("/login/and/do/something?apikey=%s&foo=bar", apiKey))
        .then()
        .statusCode(200);

    mockOkapi.setDelay(requestTimeoutMs * 2);

    RestAssured
        .get(String.format("/login/and/do/something?apikey=%s&foo=bar", apiKey))
        .then()
        .contentType(TEXT_PLAIN)
        .statusCode(408)
        .body(containsString("The timeout period of 5000ms has been exceeded while executing POST"));
  }

  public static class TestVerticleHttp extends EdgeVerticleHttp {

    @Override
    public Router defineRoutes() {
      OkapiClientFactory ocf = new OkapiClientFactory(vertx, config().getString(SYS_OKAPI_URL), config().getInteger(SYS_REQUEST_TIMEOUT_MS));
      ApiKeyHelper apiKeyHelper = new ApiKeyHelper("HEADER,PARAM,PATH");

      Router router = Router.router(vertx);
      router.route().handler(BodyHandler.create());
      router.route(HttpMethod.GET, "/admin/health").handler(this::handleHealthCheck);

      router.route(HttpMethod.GET, "/login/and/do/something")
        .handler(new GetTokenHandler(ocf, secureStore, apiKeyHelper)::handle);

      router.route(HttpMethod.GET, "/internal/server/error")
        .handler(new handle500(secureStore, ocf)::handle);
      return router;
    }
  }

  private static class GetTokenHandler extends Handler {
    public GetTokenHandler(OkapiClientFactory ocf, SecureStore secureStore, ApiKeyHelper keyHelper) {
      super(secureStore, ocf, keyHelper);
    }

    public void handle(RoutingContext ctx) {
      super.handleCommon(ctx,
              new String[] { "foo" },
              new String[] {},
              (client, params) -> {
                logger.info("Token: " + client.getToken());
                client.post(String.format("%s/echo", client.okapiURL),
                        client.tenant,
                        ctx.getBodyAsString(),
                        ctx.request().headers(),
                        resp -> handleProxyResponse(ctx, resp),
                        t -> handleProxyException(ctx, t));
              });
    }
  }

  private static class handle500 extends Handler {

    public handle500(SecureStore secureStore, OkapiClientFactory ocf) {
      super(secureStore, ocf);
    }

    public void handle(RoutingContext ctx) {
      ocf.getOkapiClient("").get("http://url.invalid.", "",
              resp -> handleProxyResponse(ctx, resp),
              t -> handleProxyException(ctx, t));
    }

  }
}
