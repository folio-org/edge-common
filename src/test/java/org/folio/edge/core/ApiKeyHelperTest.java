package org.folio.edge.core;

import static org.folio.edge.core.Constants.DEFAULT_API_KEY_SOURCES;
import static org.folio.edge.core.Constants.HEADER_API_KEY;
import static org.folio.edge.core.Constants.MSG_ACCESS_DENIED;
import static org.folio.edge.core.Constants.PATH_API_KEY;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.test.TestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@RunWith(VertxUnitRunner.class)
public class ApiKeyHelperTest {

  private static final Logger logger = LogManager.getLogger(ApiKeyHelperTest.class);
  private static TestVerticle verticle;

  public static final String headerKey = "111111";
  public static final String paramKey = "222222";
  public static final String pathKey = "333333";

  @BeforeClass
  public static void setUpOnce(TestContext ctx) throws Exception {
    verticle = new TestVerticle(TestUtils.getPort());
    verticle.start(ctx);

    RestAssured.baseURI = "http://localhost:" + verticle.port;
    RestAssured.port = verticle.port;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @AfterClass
  public static void tearDownOnce(TestContext context) throws Exception {
    verticle.close(context);
  }

  @Test
  public void testHeaderOnly(TestContext ctx) throws Exception {
    logger.info("=== Test ApiKey Source Order HEADER ===");

    verticle.setKeyHelper(new ApiKeyHelper("HEADER"));

    final Response resp1 = RestAssured
      .with()
      .header(HEADER_API_KEY, "")
      .get(String.format("/validate/%s?apikey=%s", pathKey, paramKey))
      .then()
      .statusCode(401)
      .extract()
      .response();

    assertEquals("Access Denied", resp1.body().asString());

    final Response resp2 = RestAssured
      .with()
      .get(String.format("/validate/%s?apikey=%s", pathKey, paramKey))
      .then()
      .statusCode(401)
      .extract()
      .response();

    assertEquals("Access Denied", resp2.body().asString());
  }

  @Test
  public void testHeaderParamPath(TestContext ctx) throws Exception {
    logger.info("=== Test ApiKey Source Order HEADER,PARAM,PATH ===");

    verticle.setKeyHelper(new ApiKeyHelper("HEADER,PARAM,PATH"));

    final Response resp1 = RestAssured
      .with()
      .header(HEADER_API_KEY, "APIKEY " + headerKey)
      .get(String.format("/validate/%s?apikey=%s", pathKey, paramKey))
      .then()
      .statusCode(200)
      .extract()
      .response();

    assertEquals(headerKey, resp1.body().asString());

    final Response resp2 = RestAssured
      .get(String.format("/validate/%s?apikey=%s", pathKey, paramKey))
      .then()
      .statusCode(200)
      .extract()
      .response();

    assertEquals(paramKey, resp2.body().asString());

    final Response resp3 = RestAssured
      .get(String.format("/validate/%s", pathKey))
      .then()
      .statusCode(200)
      .extract()
      .response();

    assertEquals(pathKey, resp3.body().asString());
  }

  @Test
  public void testParamHeaderPath(TestContext ctx) throws Exception {
    logger.info("=== Test ApiKey Source Order PARAM,HEADER,PATH ===");

    verticle.setKeyHelper(new ApiKeyHelper("PARAM,HEADER,PATH"));

    final Response resp1 = RestAssured
      .with()
      .header(HEADER_API_KEY, headerKey)
      .get(String.format("/validate/%s?apikey=%s", pathKey, paramKey))
      .then()
      .statusCode(200)
      .extract()
      .response();

    assertEquals(paramKey, resp1.body().asString());

    final Response resp2 = RestAssured
      .with()
      .header(HEADER_API_KEY, headerKey)
      .get(String.format("/validate/%s", pathKey))
      .then()
      .statusCode(200)
      .extract()
      .response();

    assertEquals(headerKey, resp2.body().asString());

    final Response resp3 = RestAssured
      .get(String.format("/validate/%s", pathKey))
      .then()
      .statusCode(200)
      .extract()
      .response();

    assertEquals(pathKey, resp3.body().asString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullSources() throws Exception {
    logger.info("=== Test null source list ===");
    new ApiKeyHelper(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptySources() throws Exception {
    logger.info("=== Test empty source list ===");
    new ApiKeyHelper("");
  }

  private static class TestVerticle {
    private static final Logger logger = LogManager.getLogger(TestVerticle.class);

    public final int port;
    protected final Vertx vertx;
    private ApiKeyHelper keyHelper;

    public TestVerticle(int port) {
      this.port = port;
      this.vertx = Vertx.vertx();
      this.keyHelper = new ApiKeyHelper(DEFAULT_API_KEY_SOURCES);
    }

    public void setKeyHelper(ApiKeyHelper keyHelper) {
      this.keyHelper = keyHelper;
    }

    public void close(TestContext context) {
      final Async async = context.async();
      vertx.close(res -> {
        if (res.failed()) {
          logger.error("Failed to shut down mock OKAPI server", res.cause());
          fail(res.cause().getMessage());
        } else {
          logger.info("Successfully shut down mock OKAPI server");
        }
        async.complete();
      });
    }

    protected Router defineRoutes() {
      Router router = Router.router(vertx);
      router.route(HttpMethod.GET, "/validate/:" + PATH_API_KEY).handler(this::handle);
      return router;
    }

    public void start(TestContext context) {

      HttpServer server = vertx.createHttpServer();

      final Async async = context.async();
      server.requestHandler(defineRoutes()::accept).listen(port, result -> {
        if (result.failed()) {
          logger.warn(result.cause());
        }
        context.assertTrue(result.succeeded());
        async.complete();
      });
    }

    public void handle(RoutingContext ctx) {
      String key = keyHelper.getApiKey(ctx);
      if (key == null) {
        ctx.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
          .setStatusCode(401)
          .end(MSG_ACCESS_DENIED);
      } else {
        ctx.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
          .setStatusCode(200)
          .end(key);
      }
    }
  }
}
