package org.folio.edge.core;

import static org.folio.edge.core.Constants.SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_PASSWORD;
import static org.folio.edge.core.Constants.SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_LOCATION;
import static org.folio.edge.core.Constants.SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_TYPE;
import static org.folio.edge.core.Constants.SYS_LOG_LEVEL;
import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_PORT;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.SYS_SECURE_STORE_PROP_FILE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.spy;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.edge.core.utils.ApiKeyUtils;
import org.folio.edge.core.utils.ApiKeyUtils.MalformedApiKeyException;
import org.folio.edge.core.utils.test.MockOkapi;
import org.folio.edge.core.utils.test.TestUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(VertxUnitRunner.class)
public class EdgeVerticleSslTest {
  private Vertx vertx;

  @After
  public void tearDownOnce() {
    vertx.close();
  }

  @Test
  public void setupSslConfigWithoutType(TestContext context) {
    JsonObject config = getCommonConfig()
      .put(SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_TYPE, null);

    assertDeploySuccess(context, config);
  }

  @Test
  public void setupSslConfigWithoutLocation(TestContext context) {
    JsonObject config = getCommonConfig()
      .put(SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_TYPE, "JKS")
      .put(SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_PASSWORD, "password");

    assertDeployFailure(context, config,
        IllegalStateException.class,
        "'SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_LOCATION' system param must be specified");
  }

  @Test
  public void setupSslConfigWithoutPassword(TestContext context) {
    JsonObject config = getCommonConfig()
      .put(SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_TYPE, "JKS")
      .put(SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_LOCATION, "sample_keystore.jks");

    assertDeployFailure(context, config,
        IllegalStateException.class,
        "'SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PASSWORD' system param must be specified");
  }

  @Test
  public void setupSslConfigWitInvalidPath(TestContext context) {
    JsonObject config = getCommonConfig()
      .put(SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_TYPE, "JKS")
      .put(SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_LOCATION, "some_keystore_path")
      .put(SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_PASSWORD, "password");

    assertDeployFailure(context, config,
        FileSystemException.class,
        "Unable to read file at path 'some_keystore_path'");
  }

  @Test
  public void setupSslConfigWithNotValidPassword(TestContext context) {
    JsonObject config = getCommonConfig()
      .put(SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_TYPE, "JKS")
      .put(SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_LOCATION, "sample_keystore.jks")
      .put(SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_PASSWORD, "not_valid_password");

    assertDeployFailure(context, config,
        IOException.class,
        "keystore password was incorrect");
  }

  @Test
  public void setupCorrectSslConfig(TestContext context) {
    JsonObject config = getCommonConfig()
      .put(SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_TYPE, "JKS")
      .put(SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_LOCATION, "sample_keystore.jks")
      .put(SPRING_SSL_BUNDLE_JKS_WEBSERVER_KEYSTORE_PASSWORD, "password");

    assertDeploySuccess(context, config);
  }

  private Future<String> deploy(JsonObject config) {
    int okapiPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    String apiKey = ApiKeyUtils.generateApiKey("gYn0uFv3Lf", "diku", "diku");
    try {
      knownTenants.add(ApiKeyUtils.parseApiKey(apiKey).tenantId);
    } catch (MalformedApiKeyException e) {
      return Future.failedFuture(e);
    }

    MockOkapi mockOkapi = spy(new MockOkapi(okapiPort, knownTenants));
    return mockOkapi.start()
        .compose(x -> {
          vertx = Vertx.vertx();
          final DeploymentOptions opt = new DeploymentOptions().setConfig(config);
          return vertx.deployVerticle(EdgeVerticleHttpTest.TestVerticleHttp.class.getName(), opt);
        });
  }

  private void assertDeploySuccess(TestContext context, JsonObject config) {
    deploy(config)
    .onComplete(context.asyncAssertSuccess());
  }

  private void assertDeployFailure(TestContext context, JsonObject config,
      Class<? extends Throwable> expectedType, String expectedMessage) {

    deploy(config)
    .onComplete(context.asyncAssertFailure(e -> {
      assertThat(e, instanceOf(expectedType));
      assertThat(e.getMessage(), is(expectedMessage));
    }));
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
