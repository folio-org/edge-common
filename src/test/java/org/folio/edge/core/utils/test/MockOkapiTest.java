package org.folio.edge.core.utils.test;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.core.Constants.X_OKAPI_TENANT;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;
import static org.folio.edge.core.utils.test.MockOkapi.MOCK_TOKEN;
import static org.folio.edge.core.utils.test.MockOkapi.X_DURATION;
import static org.folio.edge.core.utils.test.MockOkapi.X_ECHO_STATUS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.response.Response;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)

public class MockOkapiTest {

  private static final Logger logger = LogManager.getLogger(MockOkapiTest.class);

  private static final String tenant = "diku";

  private static MockOkapi mockOkapi;

  @Rule
  public Timeout timeout = Timeout.seconds(10);

  @BeforeClass
  public static void setUp(TestContext context) {
    int okapiPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(tenant);

    mockOkapi = new MockOkapi(okapiPort, knownTenants);
    mockOkapi.start()
    .onComplete(context.asyncAssertSuccess());

    RestAssured.baseURI = "http://localhost:" + okapiPort;
    RestAssured.port = okapiPort;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.config = RestAssured.config()
      .encoderConfig(EncoderConfig.encoderConfig()
        .appendDefaultContentCharsetToContentTypeIfUndefined(false));
  }

  @AfterClass
  public static void tearDown(TestContext context) {
    mockOkapi.close()
    .onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void testLogin(TestContext context) {
    logger.info("=== Test successful login ===");

    JsonObject payload = new JsonObject();
    payload.put("username", "admin");
    payload.put("password", "password");
    String json = payload.encode();

    Response resp = RestAssured
      .given()
      .body(json.getBytes())
      .and()
      .contentType(APPLICATION_JSON)
      .and()
      .header(X_OKAPI_TENANT, tenant)
      .and()
      .header(HttpHeaders.ACCEPT, String.format("%s, %s", APPLICATION_JSON, TEXT_PLAIN))
      .when()
      .post("/authn/login")
      .then()
      .contentType(APPLICATION_JSON)
      .statusCode(201)
      .header(X_OKAPI_TOKEN, MOCK_TOKEN)
      .extract()
      .response();

    assertEquals(json, resp.body().asString());
  }

  @Test
  public void testHealthy(TestContext context) {
    logger.info("=== Test health check ===");

    Response resp = RestAssured
      .get("/_/proxy/health")
      .then()
      .statusCode(200)
      .contentType(APPLICATION_JSON)
      .extract()
      .response();

    assertEquals("[ ]", resp.body().asString());
  }

  @Test
  public void testLoginNoPassword(TestContext context) {
    logger.info("=== Test login w/ no password ===");

    JsonObject payload = new JsonObject();
    payload.put("username", "admin");
    String json = payload.encode();

    Response resp = RestAssured
      .given()
      .body(json.getBytes())
      .and()
      .contentType(APPLICATION_JSON)
      .and()
      .header(X_OKAPI_TENANT, tenant)
      .and()
      .header(HttpHeaders.ACCEPT, String.format("%s, %s", APPLICATION_JSON, TEXT_PLAIN))
      .when()
      .post("/authn/login")
      .then()
      .contentType(TEXT_PLAIN)
      .statusCode(400)
      .extract()
      .response();

    assertEquals("Json content error", resp.body().asString());
  }

  @Test
  public void testLoginUnknownTenant(TestContext context) {
    logger.info("=== Test login w/ unknown tenant ===");

    JsonObject payload = new JsonObject();
    payload.put("username", "admin");
    payload.put("password", "password");
    String json = payload.encode();

    String unknownTenant = "bogus";

    Response resp = RestAssured
      .given()
      .body(json.getBytes())
      .and()
      .contentType(APPLICATION_JSON)
      .and()
      .header(X_OKAPI_TENANT, unknownTenant)
      .and()
      .header(HttpHeaders.ACCEPT, String.format("%s, %s", APPLICATION_JSON, TEXT_PLAIN))
      .when()
      .post("/authn/login")
      .then()
      .contentType(TEXT_PLAIN)
      .statusCode(400)
      .extract()
      .response();

    assertEquals("no such tenant " + unknownTenant, resp.body().asString());
  }

  @Test
  public void testDurationHandler(TestContext context) {
    logger.info("=== Test duration ===");

    final long duration = 3000L;

    Response resp = RestAssured
      .given()
      .header(X_DURATION, duration)
      .when()
      .get("/_/proxy/health")
      .then()
      .time(not(lessThan(duration)))
      .statusCode(200)
      .contentType(APPLICATION_JSON)
      .extract()
      .response();

    assertEquals("[ ]", resp.body().asString());
  }

  @Test
  public void testEchoHandlerWithStatus(TestContext context) {
    logger.info("=== Test echo w/ x-echo-status ===");

    int status = 201;

    JsonObject obj = new JsonObject();
    obj.put("hello", "world");

    Response resp = RestAssured
      .given()
      .header(X_ECHO_STATUS, status)
      .contentType(APPLICATION_JSON)
      .body(obj.encode())
      .when()
      .post("/echo")
      .then()
      .statusCode(status)
      .contentType(APPLICATION_JSON)
      .extract()
      .response();

    assertEquals(obj.encode(), resp.body().asString());
  }

  @Test
  public void testEchoHandlerWithoutStatus(TestContext context) {
    logger.info("=== Test echo w/o x-echo-status ===");

    Response resp = RestAssured
      .given()
      .when()
      .get("/echo")
      .then()
      .statusCode(200)
      .extract()
      .response();

    assertEquals("", resp.body().asString());
  }

  private Future<Void> assertGet200(String uri) {
    return Vertx.currentContext().executeBlocking(promise -> {
      try {
        RestAssured
        .get(new URI(uri))
        .then()
        .statusCode(200);
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
      promise.complete();
    });
  }

  @Test
  public void testVertxClose(TestContext context) {
    int port2 = TestUtils.getPort();
    var vertx2 = Vertx.vertx();
    var okapi2 = new MockOkapi(vertx2, port2, List.of());

    okapi2.close()
    .onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void testHttpClose(TestContext context) {
    int port2 = TestUtils.getPort();
    var uri = "http://localhost:" + port2 + "/_/proxy/health";
    var vertx2 = Vertx.vertx();
    var okapi2 = new MockOkapi(vertx2, port2, List.of());

    okapi2.start()
    .compose(x -> assertGet200(uri))
    .compose(x -> okapi2.close())
    .compose(x -> vertx2.createHttpClient().close())  // vertx2 still works
    .onComplete(context.asyncAssertSuccess())
    .compose(x -> assertGet200(uri))
    .onComplete(context.asyncAssertFailure());
  }

  @Test
  public void testLoginMissingUserName(TestContext context) {
    logger.info("=== Test login missing username ===");

    JsonObject payload = new JsonObject();
    payload.put("password", "password");
    String json = payload.encode();

    RestAssured
        .given()
        .body(json)
        .and()
        .contentType(APPLICATION_JSON)
        .and()
        .header(X_OKAPI_TENANT, tenant)
        .and()
        .header(HttpHeaders.ACCEPT, String.format("%s, %s", APPLICATION_JSON, TEXT_PLAIN))
        .when()
        .post("/authn/login")
        .then()
        .contentType(TEXT_PLAIN)
        .statusCode(400)
        .body(is("Json content error"));
  }

  @Test
  public void testLoginBadJson(TestContext context) {
    logger.info("=== Test login bad json ===");

    String json = "{";
    RestAssured
        .given()
        .body(json)
        .and()
        .contentType(APPLICATION_JSON)
        .and()
        .header(X_OKAPI_TENANT, tenant)
        .and()
        .header(HttpHeaders.ACCEPT, String.format("%s, %s", APPLICATION_JSON, TEXT_PLAIN))
        .when()
        .post("/authn/login")
        .then()
        .contentType(TEXT_PLAIN)
        .statusCode(400)
        .body(containsString("Failed to decode"));
  }

}
