package org.folio.edge.core;

import static org.folio.edge.core.Constants.MSG_ACCESS_DENIED;
import static org.folio.edge.core.Constants.MSG_REQUEST_TIMEOUT;
import static org.folio.edge.core.Constants.PARAM_API_KEY;
import static org.folio.edge.core.Constants.SYS_LOG_LEVEL;
import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_PORT;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.SYS_SECURE_STORE_PROP_FILE;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.core.utils.test.MockOkapi.X_DURATION;
import static org.folio.edge.core.utils.test.MockOkapi.X_ECHO_STATUS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.model.ClientInfo;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.security.SecureStore.NotFoundException;
import org.folio.edge.core.utils.ApiKeyUtils;
import org.folio.edge.core.utils.ApiKeyUtils.MalformedApiKeyException;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.edge.core.utils.OkapiClientFactory;
import org.folio.edge.core.utils.test.MockOkapi;
import org.folio.edge.core.utils.test.TestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

@RunWith(VertxUnitRunner.class)
public class EdgeVerticle2Test {

  private static final Logger logger = LogManager.getLogger(EdgeVerticle2Test.class);

  private static final String apiKey = ApiKeyUtils.generateApiKey("gYn0uFv3Lf", "diku", "diku");
  private static final String badApiKey = apiKey + "0000";
  private static final String unknownTenantApiKey = ApiKeyUtils.generateApiKey("gYn0uFv3Lf", "foobarbaz", "userA");
  private static final long requestTimeoutMs = 10000L;

  private static Vertx vertx;
  private static MockOkapi mockOkapi;

  @BeforeClass
  public static void setUpOnce(TestContext context) throws Exception {
    int okapiPort = TestUtils.getPort();
    int serverPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(ApiKeyUtils.parseApiKey(apiKey).tenantId);

    mockOkapi = spy(new MockOkapi(okapiPort, knownTenants));
    mockOkapi.start(context);

    vertx = Vertx.vertx();

    JsonObject jo = new JsonObject()
        .put(SYS_PORT, serverPort)
        .put(SYS_OKAPI_URL, "http://localhost:" + okapiPort)
        .put(SYS_SECURE_STORE_PROP_FILE, "src/main/resources/ephemeral.properties")
        .put(SYS_LOG_LEVEL, "TRACE")
        .put(SYS_REQUEST_TIMEOUT_MS, requestTimeoutMs);

    final DeploymentOptions opt = new DeploymentOptions().setConfig(jo);
    vertx.deployVerticle(TestVerticle.class.getName(), opt, context.asyncAssertSuccess());

    RestAssured.baseURI = "http://localhost:" + serverPort;
    RestAssured.port = serverPort;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @AfterClass
  public static void tearDownOnce(TestContext context) {
    logger.info("Shutting down server");
    final Async async = context.async();
    vertx.close(res -> {
      if (res.failed()) {
        logger.error("Failed to shut down edge-common server", res.cause());
        fail(res.cause().getMessage());
      } else {
        logger.info("Successfully shut down edge-common server");
      }

      logger.info("Shutting down mock Okapi");
      mockOkapi.close(context);
      async.complete();
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
      .get(String.format("/login/and/do/something?apikey=%s&foo=bar", unknownTenantApiKey))
      .then()
      .contentType(TEXT_PLAIN)
      .statusCode(403);
  }

  @Test
  public void testLoginMissingApiKey(TestContext context) throws Exception {
    logger.info("=== Test request without specifying an apiKey ===");

    RestAssured
      .get("/login/and/do/something?apikey=&foo=bar")
      .then()
      .contentType(TEXT_PLAIN)
      .statusCode(401);
  }

  @Test
  public void testLoginBadApiKey(TestContext context) throws Exception {
    logger.info("=== Test request with malformed apiKey ===");

    RestAssured
      .get(String.format("/login/and/do/something?apikey=%s&foo=bar", badApiKey))
      .then()
      .contentType(TEXT_PLAIN)
      .statusCode(401);
  }

  @Test
  public void testExceptionHandler(TestContext context) throws Exception {
    logger.info("=== Test the exception handler ===");

    RestAssured
      .get("/internal/server/error")
      .then()
      .contentType(TEXT_PLAIN)
      .statusCode(500);
  }

  @Test
  public void testMissingRequired(TestContext context) throws Exception {
    logger.info("=== Test request w/ missing required parameter ===");

    final Response resp = RestAssured
      .with()
      .header(HttpHeaders.CONTENT_TYPE.toString(), TEXT_PLAIN)
      .body("success")
      .get(String.format("/login/and/do/something?apikey=%s", apiKey))
      .then()
      .contentType(TEXT_PLAIN)
      .statusCode(400)
      .extract()
      .response();

    assertEquals("Missing required parameter: foo", resp.body().asString());
  }

  @Test
  public void test404(TestContext context) throws Exception {
    logger.info("=== Test 404 rseponse ===");

    final Response resp = RestAssured
      .with()
      .header(HttpHeaders.CONTENT_TYPE.toString(), TEXT_PLAIN)
      .header(X_ECHO_STATUS, "404")
      .body("Not Found")
      .get(String.format("/login/and/do/something?apikey=%s&foo=bar", apiKey))
      .then()
      .contentType(TEXT_PLAIN)
      .statusCode(404)
      .extract()
      .response();

    assertEquals("Not Found", resp.body().asString());
  }

  @Test
  public void testCachedToken(TestContext context) throws Exception {
    logger.info("=== Test the tokens are cached and reused ===");

    int iters = 5;

    for (int i = 0; i < iters; i++) {
      final Response resp = RestAssured
        .with()
        .header(HttpHeaders.CONTENT_TYPE.toString(), TEXT_PLAIN)
        .body("success")
        .get(String.format("/login/and/do/something?apikey=%s&foo=bar", apiKey))
        .then()
        .contentType(TEXT_PLAIN)
        .statusCode(200)
        .extract()
        .response();

      assertEquals("success", resp.body().asString());
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

    assertEquals(MSG_REQUEST_TIMEOUT, resp.body().asString());
  }

  public static class TestVerticle extends EdgeVerticle2 {

    @Override
    public Router defineRoutes() {
      OkapiClientFactory ocf = new OkapiClientFactory(vertx, config().getString(SYS_OKAPI_URL), config().getLong(SYS_REQUEST_TIMEOUT_MS));
      InstitutionalUserHelper iuHelper = new InstitutionalUserHelper(secureStore);
      ApiKeyHelper apiKeyHelper = new ApiKeyHelper("HEADER,PARAM,PATH");

      Router router = Router.router(vertx);
      router.route().handler(BodyHandler.create());
      router.route(HttpMethod.GET, "/admin/health").handler(this::handleHealthCheck);
      router.route(HttpMethod.GET, "/always/login")
        .handler(new GetTokenHandler(iuHelper, ocf, secureStore, apiKeyHelper, false)::handle);
      router.route(HttpMethod.GET, "/login/and/do/something")
        .handler(new GetTokenHandler(iuHelper, ocf, secureStore, apiKeyHelper, true)::handle);
      router.route(HttpMethod.GET, "/internal/server/error")
        .handler(new handle500(secureStore, ocf)::handle);
      return router;
    }
  }

  private static class GetTokenHandler extends Handler {
    public final boolean useCache;

    public GetTokenHandler(InstitutionalUserHelper iuHelper, OkapiClientFactory ocf, SecureStore secureStore,
        ApiKeyHelper keyHelper, boolean useCache) {
      super(secureStore, ocf, keyHelper);
      this.useCache = useCache;
    }

    public void handle(RoutingContext ctx) {
      if (useCache) {
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
      } else {
        ClientInfo clientInfo;
        try {
          clientInfo = ApiKeyUtils.parseApiKey(ctx.request().getParam(PARAM_API_KEY));
        } catch (MalformedApiKeyException e) {
          accessDenied(ctx, MSG_ACCESS_DENIED);
          return;
        }

        String password = null;
        try {
          password = iuHelper.secureStore.get(clientInfo.salt, clientInfo.tenantId, clientInfo.username);
        } catch (NotFoundException e) {
          accessDenied(ctx, MSG_ACCESS_DENIED);
          return;
        }

        final OkapiClient client = ocf.getOkapiClient(clientInfo.tenantId);
        client.login(clientInfo.username, password, ctx.request().headers())
          .thenAcceptAsync(token -> {
            logger.info("Token: " + token);
            client.post(String.format("%s/echo", client.okapiURL),
                client.tenant,
                ctx.getBodyAsString(),
                ctx.request().headers(),
                resp -> handleProxyResponse(ctx, resp),
                t -> handleProxyException(ctx, t));
          })
          .exceptionally(t -> {
            if (t.getCause() instanceof TimeoutException) {
              requestTimeout(ctx, MSG_REQUEST_TIMEOUT);
            } else {
              accessDenied(ctx, MSG_ACCESS_DENIED);
            }
            return null;
          });
      }
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
