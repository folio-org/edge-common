package org.folio.edge.core;

import static io.restassured.config.DecoderConfig.decoderConfig;
import static org.folio.edge.core.Constants.SYS_LOG_LEVEL;
import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_PORT;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.SYS_RESPONSE_COMPRESSION;
import static org.folio.edge.core.Constants.SYS_SECURE_STORE_PROP_FILE;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.spy;

import io.restassured.RestAssured;
import io.restassured.config.DecoderConfig.ContentDecoder;
import io.restassured.http.Header;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.ApiKeyUtils;
import org.folio.edge.core.utils.test.MockOkapi;
import org.folio.edge.core.utils.test.TestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(VertxUnitRunner.class)
public class ResponseCompressionTest {

  private static final Logger logger = LogManager.getLogger(ResponseCompressionTest.class);

  private static final String apiKey = "eyJzIjoiZ0szc0RWZ3labCIsInQiOiJkaWt1IiwidSI6ImRpa3UifQ==";
  private static final long requestTimeoutMs = 10000L;

  private static Vertx vertx;
  private static MockOkapi mockOkapi;

  @BeforeClass
  public static void setUpOnce(TestContext context) throws Exception {
    int okapiPort = TestUtils.getPort();
    int serverPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(ApiKeyUtils.parseApiKey(apiKey).tenantId);

    vertx = Vertx.vertx();

    mockOkapi = spy(new MockOkapi(vertx, okapiPort, knownTenants));
    mockOkapi.start()
    .onComplete(context.asyncAssertSuccess());

    JsonObject jo = new JsonObject()
            .put(SYS_PORT, serverPort)
            .put(SYS_OKAPI_URL, "http://localhost:" + okapiPort)
            .put(SYS_SECURE_STORE_PROP_FILE, "src/main/resources/ephemeral.properties")
            .put(SYS_LOG_LEVEL, "TRACE")
            .put(SYS_REQUEST_TIMEOUT_MS, requestTimeoutMs)
            .put(SYS_RESPONSE_COMPRESSION, true);

    final DeploymentOptions opt = new DeploymentOptions().setConfig(jo);
    vertx.deployVerticle(EdgeVerticleHttpTest.TestVerticleHttp.class.getName(), opt, context.asyncAssertSuccess());

    RestAssured.baseURI = "http://localhost:" + serverPort;
    RestAssured.port = serverPort;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @AfterClass
  public static void tearDownOnce(TestContext context) {
    logger.info("Shutting down server");
    vertx.close()  // this automatically shuts down mockOkapi
    .onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void testResponseGzip() {
    logger.info("=== Test response GZip compression ===");

    RestAssured.given()
      .config(RestAssured.config().decoderConfig(decoderConfig().contentDecoders(ContentDecoder.GZIP)))
    .when()
       .get("/admin/health")
    .then()
       .contentType(TEXT_PLAIN)
       .statusCode(200)
       .header(HttpHeaders.CONTENT_ENCODING, "gzip")
       .body(is("\"OK\""));
  }

  @Test
  public void testResponseDeflate() {
    logger.info("=== Test response GZip compression ===");

    RestAssured.given()
      .config(RestAssured.config().decoderConfig(decoderConfig().contentDecoders(ContentDecoder.DEFLATE)))
    .when()
       .get("/admin/health")
    .then()
       .contentType(TEXT_PLAIN)
       .statusCode(200)
       .header(HttpHeaders.CONTENT_ENCODING, "deflate")
       .body(is("\"OK\""));
  }

  @Test
  public void testResponseNoCompressionHeaderInstance(TestContext context) {
    logger.info("=== Test no compression (Accept-Encoding: instance) ===");

    RestAssured.given()
        .config(RestAssured.config().decoderConfig(decoderConfig().noContentDecoders()))
        .header(new Header(org.apache.http.HttpHeaders.ACCEPT_ENCODING, "instance"))
      .when()
        .get("/admin/health")
      .then()
        .contentType(TEXT_PLAIN)
        .statusCode(200)
        .header(HttpHeaders.CONTENT_ENCODING, (String) null)
        .body(is("\"OK\""));
  }

  @Test
  public void testResponseNoCompressionWithoutAcceptEncoding(TestContext context) {
    logger.info("=== Test no compression (without Accept-Encoding header) ===");

    RestAssured.given()
      .config(RestAssured.config().decoderConfig(decoderConfig().noContentDecoders()))
    .when()
      .get("/admin/health")
    .then()
      .contentType(TEXT_PLAIN)
      .statusCode(200)
      .header(HttpHeaders.CONTENT_ENCODING, (String) null)
      .body(is("\"OK\""));
  }
}
