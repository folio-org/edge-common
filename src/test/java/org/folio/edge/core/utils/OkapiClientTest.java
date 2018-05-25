package org.folio.edge.core.utils;

import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;
import static org.folio.edge.core.utils.test.MockOkapi.MOCK_TOKEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.folio.edge.core.utils.test.MockOkapi;
import org.folio.edge.core.utils.test.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class OkapiClientTest {

  private static final Logger logger = Logger.getLogger(OkapiClientTest.class);

  private static final String tenant = "diku";
  private static final long reqTimeout = 3000L;

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

    client = new OkapiClientFactory(Vertx.vertx(), "http://localhost:" + okapiPort, reqTimeout).getOkapiClient(tenant);
  }

  @After
  public void tearDown(TestContext context) {
    mockOkapi.close();
  }

  @Test
  public void testLogin(TestContext context) throws Exception {
    logger.info("=== Test successful login === ");

    assertNotNull(client.login("admin", "password").get());

    // Ensure that the client's default headers now contain the
    // x-okapi-token for use in subsequent okapi calls
    assertEquals(MOCK_TOKEN, client.defaultHeaders.get(X_OKAPI_TOKEN));
  }

  @Test
  public void testHealthy(TestContext context) throws Exception {
    logger.info("=== Test health check === ");

    assertTrue(client.healthy().get());
  }

  @Test
  public void testLoginNoPassword(TestContext context) throws Exception {
    logger.info("=== Test login w/ no password === ");

    assertNull(client.login("admin", null).get());
    assertNull(client.defaultHeaders.get(X_OKAPI_TOKEN));
  }
}
