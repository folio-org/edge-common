package org.folio.edge.core;

import static org.folio.edge.core.Constants.SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PASSWORD;
import static org.folio.edge.core.Constants.SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PATH;
import static org.folio.edge.core.Constants.SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_TYPE;
import static org.folio.edge.core.Constants.SYS_LOG_LEVEL;
import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_PORT;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.SYS_SECURE_STORE_PROP_FILE;
import static org.mockito.Mockito.spy;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.edge.core.utils.ApiKeyUtils;
import org.folio.edge.core.utils.test.MockOkapi;
import org.folio.edge.core.utils.test.TestUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(VertxUnitRunner.class)
public class EdgeVerticleSslTest {
  private Vertx vertx;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @After
  public void tearDownOnce() {
    vertx.close();
  }

  @Test
  public void setupSslConfigWithoutType(TestContext context) throws Exception {
    JsonObject config = getCommonConfig()
      .put(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_TYPE, null);

    deployVerticle(context, config);
  }

  @Test
  public void setupSslConfigWithoutPassword(TestContext context) throws Exception {
    JsonObject config = getCommonConfig()
      .put(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_TYPE, "JKS")
      .put(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PATH, "sample_keystore.jks");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("'SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PASSWORD' system param must be specified");

    deployVerticle(context, config);
  }

  @Test
  public void setupSslConfigWitInvalidPath(TestContext context) throws Exception {
    JsonObject config = getCommonConfig()
      .put(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_TYPE, "JKS")
      .put(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PATH, "some_keystore_path")
      .put(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PASSWORD, "password");

    thrown.expect(FileSystemException.class);
    thrown.expectMessage("Unable to read file at path 'some_keystore_path'");

    deployVerticle(context, config);
  }

  @Test
  public void setupSslConfigWithNotValidPassword(TestContext context) throws Exception {
    JsonObject config = getCommonConfig()
      .put(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_TYPE, "JKS")
      .put(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PATH, "sample_keystore.jks")
      .put(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PASSWORD, "not_valid_password");

    thrown.expect(IOException.class);
    thrown.expectMessage("keystore password was incorrect");

    deployVerticle(context, config);
  }

  @Test
  public void setupCorrectSslConfig(TestContext context) throws Exception {
    JsonObject config = getCommonConfig()
      .put(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_TYPE, "JKS")
      .put(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PATH, "sample_keystore.jks")
      .put(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PASSWORD, "password");

    deployVerticle(context, config);
  }

  private void deployVerticle(TestContext context, JsonObject config) throws ApiKeyUtils.MalformedApiKeyException {
    int okapiPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    String apiKey = ApiKeyUtils.generateApiKey("gYn0uFv3Lf", "diku", "diku");
    knownTenants.add(ApiKeyUtils.parseApiKey(apiKey).tenantId);

    MockOkapi mockOkapi = spy(new MockOkapi(okapiPort, knownTenants));
    mockOkapi.start()
      .onComplete(context.asyncAssertSuccess());

    vertx = Vertx.vertx();

    final DeploymentOptions opt = new DeploymentOptions().setConfig(config);
    vertx.deployVerticle(EdgeVerticleHttpTest.TestVerticleHttp.class.getName(), opt, context.asyncAssertSuccess());
  }

  private JsonObject getCommonConfig() {
    int serverPort = TestUtils.getPort();
    int okapiPort = TestUtils.getPort();
    return new JsonObject()
      .put(SYS_PORT, serverPort)
      .put(SYS_OKAPI_URL, "http://localhost:" + okapiPort)
      .put(SYS_SECURE_STORE_PROP_FILE, "src/main/resources/ephemeral.properties")
      .put(SYS_LOG_LEVEL, "TRACE")
      .put(SYS_REQUEST_TIMEOUT_MS, 5000);
  }
}
