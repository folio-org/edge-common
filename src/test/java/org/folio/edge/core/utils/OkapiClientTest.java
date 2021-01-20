package org.folio.edge.core.utils;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.HEADER_API_KEY;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;
import static org.folio.edge.core.utils.test.MockOkapi.MOCK_TOKEN;
import static org.folio.edge.core.utils.test.MockOkapi.X_ECHO_STATUS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.test.MockOkapi;
import org.folio.edge.core.utils.test.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class OkapiClientTest {

  private static final Logger logger = LogManager.getLogger(OkapiClientTest.class);

  private static final String tenant = "diku";
  private static final long reqTimeout = 3000L;

  private OkapiClientFactory ocf;
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

    ocf = new OkapiClientFactory(Vertx.vertx(), "http://localhost:" + okapiPort, reqTimeout);
    client = ocf.getOkapiClient(tenant);
  }

  @After
  public void tearDown(TestContext context) {
    mockOkapi.close(context);
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
  public void testLoginFailure(TestContext context) throws Exception {
    logger.info("=== Test successful login === ");

    OkapiClient client = ocf.getOkapiClient("");
    assertNull(client.login("admin", "password").get());

    // Ensure that the client's default headers now contain the
    // x-okapi-token for use in subsequent okapi calls
    assertNull(client.defaultHeaders.get(X_OKAPI_TOKEN));
  }

  @Test
  public void testCopyConstructor() throws Exception {
    logger.info("=== Test copy constructor === ");

    client.setToken("foobarbaz");

    OkapiClient copy = new OkapiClient(client);

    assertEquals(client.tenant, copy.tenant);
    assertEquals(client.okapiURL, copy.okapiURL);
    assertEquals(client.getToken(), copy.getToken());
    assertEquals(client.reqTimeout, copy.reqTimeout);
    assertEquals(client.client, copy.client);
  }

  @Test
  public void testHealthy(TestContext context) throws Exception {
    logger.info("=== Test health check === ");

    assertTrue(client.healthy().get());
  }

  @Test
  public void testLoginNoUsername(TestContext context) throws Exception {
    logger.info("=== Test login w/ no password === ");

    assertNull(client.login(null, "password").get());
    assertNull(client.defaultHeaders.get(X_OKAPI_TOKEN));
  }

  @Test
  public void testLoginNoPassword(TestContext context) throws Exception {
    logger.info("=== Test login w/ no password === ");

    assertNull(client.login("admin", null).get());
    assertNull(client.defaultHeaders.get(X_OKAPI_TOKEN));
  }

  @Test
  public void testDeleteWithHeaders(TestContext context) throws Exception {
    logger.info("=== Test delete w/ headers === ");

    int status = 204;

    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.set(X_ECHO_STATUS, String.valueOf(status));

    Async async = context.async();
    client.delete(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
        tenant,
        headers,
        resp -> resp.bodyHandler(body -> {
          context.assertEquals(status, resp.statusCode());
          async.complete();
        }),
        t -> {
          context.fail(t);
        });
  }

  @Test
  public void testPutWithHeaders(TestContext context) throws Exception {
    logger.info("=== Test put w/ headers === ");

    int status = 204;

    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.set(X_ECHO_STATUS, String.valueOf(status));

    Async async = context.async();
    client.put(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
        tenant,
        headers,
        resp -> resp.bodyHandler(body -> {
          context.assertEquals(status, resp.statusCode());
          async.complete();
        }),
        t -> {
          context.fail(t);
        });
  }

  @Test
  public void testPostWithHeaders(TestContext context) throws Exception {
    logger.info("=== Test post w/ headers === ");

    int status = 201;

    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.set(X_ECHO_STATUS, String.valueOf(status));
    headers.set(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);

    JsonObject obj = new JsonObject();
    obj.put("hello", "world");

    Async async = context.async();
    client.post(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
        tenant,
        obj.encode(),
        headers,
        resp -> resp.bodyHandler(body -> {
          context.assertEquals(status, resp.statusCode());
          context.assertEquals(obj.encode(), body.toString());
          context.assertEquals(APPLICATION_JSON, resp.getHeader(HttpHeaders.CONTENT_TYPE));
          async.complete();
        }),
        t -> {
          context.fail(t);
        });
  }

  @Test
  public void testGetWithHeaders(TestContext context) throws Exception {
    logger.info("=== Test get w/ headers === ");

    int status = 404;

    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.set(X_ECHO_STATUS, String.valueOf(status));
    headers.set(HEADER_API_KEY, "foobarbaz");

    Async async = context.async();
    client.get(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
        tenant,
        headers,
        resp -> resp.bodyHandler(body -> {
          context.assertEquals(status, resp.statusCode());
          context.assertNull(resp.headers().get(HEADER_API_KEY));
          async.complete();
        }),
        t -> {
          context.fail(t);
        });
  }

  @Test
  public void testDeleteWithoutHeaders(TestContext context) throws Exception {
    logger.info("=== Test delete w/o headers === ");

    Async async = context.async();
    client.delete(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
        tenant,
        resp -> resp.bodyHandler(body -> {
          context.assertEquals(200, resp.statusCode());
          async.complete();
        }),
        t -> {
          context.fail(t);
        });
  }

  @Test
  public void testPutWithoutHeaders(TestContext context) throws Exception {
    logger.info("=== Test put w/o headers === ");

    Async async = context.async();
    client.put(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
        tenant,
        resp -> resp.bodyHandler(body -> {
          context.assertEquals(200, resp.statusCode());
          async.complete();
        }),
        t -> {
          context.fail(t);
        });
  }

  @Test
  public void testPostWithoutHeaders(TestContext context) throws Exception {
    logger.info("=== Test post w/o headers === ");

    JsonObject obj = new JsonObject();
    obj.put("hello", "world");

    Async async = context.async();
    client.post(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
        tenant,
        obj.encode(),
        resp -> resp.bodyHandler(body -> {
          context.assertEquals(200, resp.statusCode());
          context.assertEquals(obj.encode(), body.toString());
          async.complete();
        }),
        t -> {
          context.fail(t);
        });
  }

  @Test
  public void testGetWithoutHeaders(TestContext context) throws Exception {
    logger.info("=== Test get w/o headers === ");

    Async async = context.async();
    client.get(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
        tenant,
        resp -> resp.bodyHandler(body -> {
          context.assertEquals(200, resp.statusCode());
          async.complete();
        }),
        t -> {
          context.fail(t);
        });
  }
}
