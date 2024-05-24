package org.folio.edge.core;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.edge.core.utils.OkapiClientFactory;
import org.folio.edge.core.utils.OkapiClientFactoryInitializer;
import org.folio.edge.core.utils.SslConfigurationUtil;
import org.folio.edge.core.utils.test.TestUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.Security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class EdgeVerticleTlsIntegrationTest {

  private static final Logger logger = LogManager.getLogger(EdgeVerticleTlsIntegrationTest.class);
  private static Vertx vertx;

  private static final String KEYSTORE_TYPE = "BCFKS";
  private static final String KEYSTORE_PATH = "test.keystore 1.bcfks";
  private static final String TRUST_STORE_PATH = "test.truststore 1.bcfks";
  private static final String KEYSTORE_PASSWORD = "SecretPassword";
  private static final String RESPONSE_MESSAGE = "<OK>";
  private static final String OKAPI_URL = "http://localhost:" + TestUtils.getPort();
  private static final String TENANT = "diku";

  @Before
  public void setUpOnce() {
    Security.addProvider(new BouncyCastleFipsProvider());
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testServerClientTlsCommunication(TestContext context) throws IllegalAccessException {
    final JsonObject config = getCommonConfig(true);

    final HttpServerOptions serverOptions = new HttpServerOptions();
    serverOptions.setPort(config.getInteger(Constants.SYS_PORT));

    SslConfigurationUtil.configureSslServerOptionsIfEnabled(config, serverOptions);

    final HttpServer httpServer = vertx.createHttpServer(serverOptions);
    httpServer
      .requestHandler(req -> req.response().putHeader(HttpHeaders.CONTENT_TYPE, Constants.TEXT_PLAIN).end(RESPONSE_MESSAGE))
      .listen(config.getInteger(Constants.SYS_PORT), getCommonServerHandler(config));

    final OkapiClientFactory okapiClientFactory = OkapiClientFactoryInitializer.createInstance(vertx, config);
    final OkapiClient okapiClient = okapiClientFactory.getOkapiClient(TENANT);
    final WebClientOptions webClientOptions = (WebClientOptions) FieldUtils.readDeclaredField(okapiClient.client, "options", true);

    assertTrue(webClientOptions.isSsl());
    assertNotNull(webClientOptions.getTrustOptions());

    createClientRequest(context, webClientOptions, config);
  }

  @Test(expected = java.net.ConnectException.class)
  public void testFailingServerClientTlsCommunication(TestContext context) throws IllegalAccessException {
    final JsonObject config = getCommonConfig(false);

    final HttpServerOptions serverOptions = new HttpServerOptions();
    serverOptions.setPort(config.getInteger(Constants.SYS_PORT));

    SslConfigurationUtil.configureSslServerOptionsIfEnabled(config, serverOptions);

    final HttpServer httpServer = vertx.createHttpServer(serverOptions);
    httpServer
      .requestHandler(req -> req.response().putHeader(HttpHeaders.CONTENT_TYPE, Constants.TEXT_PLAIN).end(RESPONSE_MESSAGE))
      .listen(config.getInteger(Constants.SYS_PORT), getCommonServerHandler(config));

    final OkapiClientFactory okapiClientFactory = OkapiClientFactoryInitializer.createInstance(vertx, config);
    final OkapiClient okapiClient = okapiClientFactory.getOkapiClient(TENANT);
    final WebClientOptions webClientOptions = (WebClientOptions) FieldUtils.readDeclaredField(okapiClient.client, "options", true);

    assertFalse(webClientOptions.isSsl());
    assertNull(webClientOptions.getTrustOptions());

    createClientRequest(context, webClientOptions, config);
  }

  private static io.vertx.core.@NotNull Handler<AsyncResult<HttpServer>> getCommonServerHandler(JsonObject config) {
    return http -> logger.info("Server started on port {}", config.getInteger(Constants.SYS_PORT));
  }

  private static void createClientRequest(TestContext context, WebClientOptions webClientOptions, JsonObject config) {
    final WebClient webClient = WebClient.create(vertx, webClientOptions);
    webClient.get(config.getInteger(Constants.SYS_PORT), "localhost", "/")
      .send()
      .onComplete(context.asyncAssertSuccess(response -> {
        String message = response.body().toString();
        logger.info("WebClient sent message to server port {}, response message: {}", config.getInteger(Constants.SYS_PORT), message);
        context.assertEquals(HttpResponseStatus.OK.code(), response.statusCode());
        context.assertEquals(RESPONSE_MESSAGE, message);
      }));
  }

  private JsonObject getCommonConfig(boolean enableWebClientSsl) {
    int serverPort = TestUtils.getPort();
    JsonObject config = new JsonObject().put(Constants.SYS_PORT, serverPort)
      .put(Constants.SYS_OKAPI_URL, OKAPI_URL)
      .put(Constants.SYS_SECURE_STORE_PROP_FILE, "src/main/resources/ephemeral.properties")
      .put(Constants.SYS_LOG_LEVEL, "TRACE")
      .put(Constants.SYS_REQUEST_TIMEOUT_MS, 5000)
      .put(Constants.SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_TYPE, KEYSTORE_TYPE)
      .put(Constants.SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_LOCATION, KEYSTORE_PATH)
      .put(Constants.SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PASSWORD, KEYSTORE_PASSWORD);
    if (enableWebClientSsl) {
      return config
        .put(Constants.FOLIO_CLIENT_TLS_ENABLED, true)
        .put(Constants.FOLIO_CLIENT_TLS_TRUST_STORE_TYPE, KEYSTORE_TYPE)
        .put(Constants.FOLIO_CLIENT_TLS_TRUST_STORE_PATH, TRUST_STORE_PATH)
        .put(Constants.FOLIO_CLIENT_TLS_TRUST_STORE_PASSWORD, KEYSTORE_PASSWORD);
    } else {
      return config
        .put(Constants.FOLIO_CLIENT_TLS_ENABLED, false);
    }
  }
}
