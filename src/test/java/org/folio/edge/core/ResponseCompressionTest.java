package org.folio.edge.core;

import io.restassured.RestAssured;
import io.restassured.config.DecoderConfig;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
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

import static io.restassured.config.DecoderConfig.decoderConfig;
import static org.folio.edge.core.Constants.SYS_LOG_LEVEL;
import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_PORT;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.SYS_RESPONSE_COMPRESSION;
import static org.folio.edge.core.Constants.SYS_SECURE_STORE_PROP_FILE;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;

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

        mockOkapi = spy(new MockOkapi(okapiPort, knownTenants));
        mockOkapi.start(context);

        vertx = Vertx.vertx();

        JsonObject jo = new JsonObject()
                .put(SYS_PORT, serverPort)
                .put(SYS_OKAPI_URL, "http://localhost:" + okapiPort)
                .put(SYS_SECURE_STORE_PROP_FILE, "src/main/resources/ephemeral.properties")
                .put(SYS_LOG_LEVEL, "TRACE")
                .put(SYS_REQUEST_TIMEOUT_MS, requestTimeoutMs)
                .put(SYS_RESPONSE_COMPRESSION, true);

        final DeploymentOptions opt = new DeploymentOptions().setConfig(jo);
        vertx.deployVerticle(EdgeVerticleTest.TestVerticle.class.getName(), opt, context.asyncAssertSuccess());

        RestAssured.baseURI = "http://localhost:" + serverPort;
        RestAssured.port = serverPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterClass
    public static void tearDownOnce(TestContext context) {
        final Async async = context.async();
        logger.info("Shutting down server");
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
    public void testResponseCompression(TestContext context) {
        logger.info("=== Test response compression (Accept-Encoding: gzip / Accept-Encoding: deflate)  ===");

        for (DecoderConfig.ContentDecoder type : DecoderConfig.ContentDecoder.values()) {
            final Response resp = RestAssured.given()
                    .config(RestAssured.config().decoderConfig(decoderConfig().contentDecoders(type)))
                    .get("/admin/health")
                    .then()
                    .contentType(TEXT_PLAIN)
                    .statusCode(200)
                    .header(HttpHeaders.CONTENT_ENCODING.toString(), type.name().toLowerCase())
                    .extract()
                    .response();
            assertEquals("\"OK\"", resp.body().asString());
        }
    }

    @Test
    public void testResponseNoCompressionHeaderInstance(TestContext context) {
        logger.info("=== Test no compression (Accept-Encoding: instance) ===");

        final Response resp = RestAssured.given()
                .config(RestAssured.config().decoderConfig(decoderConfig().noContentDecoders()))
                .header(new Header(org.apache.http.HttpHeaders.ACCEPT_ENCODING, "instance"))
                .get("/admin/health")
                .then()
                .contentType(TEXT_PLAIN)
                .statusCode(200)
                .extract()
                .response();
        assertEquals("\"OK\"", resp.body().asString());
    }

    @Test
    public void testResponseNoCompressionWithoutAcceptEncoding(TestContext context) {
        logger.info("=== Test no compression (without Accept-Encoding header) ===");

        final Response resp = RestAssured.given()
                .config(RestAssured.config().decoderConfig(decoderConfig().noContentDecoders()))
                .get("/admin/health")
                .then()
                .contentType(TEXT_PLAIN)
                .statusCode(200)
                .extract()
                .response();
        assertEquals("\"OK\"", resp.body().asString());
    }
}
