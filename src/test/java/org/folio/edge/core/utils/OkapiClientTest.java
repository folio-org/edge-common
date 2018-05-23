package org.folio.edge.core.utils;

import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class OkapiClientTest {

  private static final Logger logger = Logger.getLogger(OkapiClientTest.class);

  private final String mockToken = MockOkapi.mockToken;
  private static final String tenant = "diku";
  private static final long reqTimeout = 3000L;

  private Vertx vertx;
  private OkapiClient client;
  private MockOkapi mockOkapi;

  // ** setUp/tearDown **//

  @Before
  public void setUp(TestContext context) throws Exception {
    int okapiPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(tenant);

    mockOkapi = new MockOkapi(okapiPort, knownTenants);
    mockOkapi.start(context);
    vertx = Vertx.vertx();

    client = new OkapiClientFactory(vertx, "http://localhost:" + okapiPort, reqTimeout).getOkapiClient(tenant);
  }

  @After
  public void tearDown(TestContext context) {
    logger.info("Closing Vertx");
    vertx.close(context.asyncAssertSuccess());
  }

  // ** Test cases **//

  @Test
  public void testLogin(TestContext context) throws Exception {
    client.login("admin", "password").get();

    // Ensure that the client's default headers now contain the
    // x-okapi-token for use in subsequent okapi calls
    assertEquals(mockToken, client.defaultHeaders.get(X_OKAPI_TOKEN));
  }

  @Test
  public void testHealthy(TestContext context) {
    Async async = context.async();
    client.healthy().thenAccept(isHealthy -> {
      assertTrue(isHealthy);
      async.complete();
    });
  }

  @Test
  public void testLoginNoPassword(TestContext context) {
    Async async = context.async();
    // client.getToken("admin", "password").thenRun(() -> {
    CompletableFuture<String> future = client.login("admin", null);
    future.thenAcceptAsync(token -> {
      assertNull(token);
    });
    async.complete();
  }
}
