package org.folio.edge.core.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.cache.TokenCacheFactory;
import org.folio.edge.core.utils.test.MockOkapi;
import org.folio.edge.core.utils.test.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(VertxUnitRunner.class)
public class SslOkapiClientTest {
  private static final Logger logger = LogManager.getLogger(SslOkapiClientTest.class);

  private static final String tenant = "diku";
  public static final String KEYSTORE_TYPE = "JKS";
  public static final String KEYSTORE_PATH = "sample_keystore.jks";
  public static final String KEYSTORE_PASSWORD = "password";

  private OkapiClientFactory ocf;
  private OkapiClient client;
  private MockOkapi mockOkapi;
  private Vertx vertx;

  @Before
  public void setUp(TestContext context) {
    int okapiPort = TestUtils.getPort();
    vertx = Vertx.vertx();

    startSslMockServer(vertx, context, okapiPort);
    initSslOkapiClient(vertx, okapiPort);
    TokenCacheFactory.initialize(100);
  }

  @After
  public void tearDown(TestContext context) {
    mockOkapi.close()
      .onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void testLogin() throws Exception {
    logger.info("=== Test successful login === ");

    assertNotNull(client.login("admin", "password").get());
    assertThat(client.login("admin", "password").get(), is(MockOkapi.MOCK_TOKEN));
  }

  @Test
  public void testHealthy() throws Exception {
    logger.info("=== Test health check === ");

    assertTrue(client.healthy().get());
  }

  private void startSslMockServer(Vertx vertx, TestContext context, int okapiPort) {
    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(tenant);

    KeyCertOptions keyStoreOptions = new KeyStoreOptions().setType(KEYSTORE_TYPE).setPath(KEYSTORE_PATH).setPassword("password");

    mockOkapi = new MockOkapi(vertx, okapiPort, knownTenants);
    HttpServerOptions options = new HttpServerOptions()
      .setCompressionSupported(true)
      .setSsl(true)
      .setKeyCertOptions(keyStoreOptions);
    mockOkapi.start(options)
      .onComplete(context.asyncAssertSuccess());
  }

  private void initSslOkapiClient(Vertx vertx, int okapiPort) {
    ocf = new OkapiClientFactory(vertx, "http://localhost:" + okapiPort, 5000,
      KEYSTORE_TYPE, "SUN", KEYSTORE_PATH, KEYSTORE_PASSWORD, null, null);
    client =  ocf.getOkapiClient(tenant);
  }
}
