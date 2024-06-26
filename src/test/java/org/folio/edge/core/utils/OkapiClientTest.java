package org.folio.edge.core.utils;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.HEADER_API_KEY;
import static org.folio.edge.core.Constants.X_OKAPI_TENANT;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;
import static org.folio.edge.core.utils.test.MockOkapi.X_DURATION;
import static org.folio.edge.core.utils.test.MockOkapi.X_ECHO_STATUS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import io.vertx.core.net.KeyStoreOptions;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.cache.TokenCacheFactory;
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
  private static final String secondaryTenant = "diku_second";
  private static final int reqTimeout = 300;

  private OkapiClientFactory ocf;
  private OkapiClient client;
  private MockOkapi mockOkapi;

  // ** setUp/tearDown **//

  @Before
  public void setUp(TestContext context) {
    int okapiPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(tenant);

    mockOkapi = new MockOkapi(okapiPort, knownTenants);
    mockOkapi.start()
    .onComplete(context.asyncAssertSuccess());

    TokenCacheFactory.initialize(100);

    ocf = new OkapiClientFactory(Vertx.vertx(), "http://localhost:" + okapiPort, reqTimeout);
    client = ocf.getOkapiClient(tenant);
  }

  @After
  public void tearDown(TestContext context) {
    mockOkapi.close()
    .onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void testLogin(TestContext context) throws Exception {
    logger.info("=== Test successful login === ");

    assertNotNull(client.login("admin", "password").get());
    assertThat(client.login("admin", "password").get(), is(MockOkapi.MOCK_TOKEN));
  }

  @Test
  public void testLoginFailure(TestContext context) {
    logger.info("=== Test unsuccessful login === ");
    OkapiClient client = ocf.getOkapiClient("x");
    client.doLogin("admin", "password")
            .onComplete(context.asyncAssertFailure(res -> {
              assertEquals("POST /authn/login returned status 400: no such tenant x", res.getMessage());
            }));
  }

  @Test
  public void testCopyConstructor() {
    logger.info("=== Test copy constructor === ");

    assertFalse(client.defaultHeaders.contains(X_OKAPI_TOKEN));

    client.setToken("foobarbaz");
    assertEquals("foobarbaz", client.defaultHeaders.get(X_OKAPI_TOKEN));

    client.setToken(null);
    assertFalse(client.defaultHeaders.contains(X_OKAPI_TOKEN));

    OkapiClient copy = new OkapiClient(client);

    assertEquals(client.tenant, copy.tenant);
    assertEquals(client.okapiURL, copy.okapiURL);
    assertEquals(client.getToken(), copy.getToken());
    assertEquals(client.reqTimeout, copy.reqTimeout);
    assertEquals(client.client, copy.client);
  }

  @Test
  public void testHealthy() throws Exception {
    logger.info("=== Test health check === ");

    assertTrue(client.healthy().get());
  }

  @Test
  public void testHealthyNoHost() throws Exception {
    int freePort = TestUtils.getPort();
    var factory = new OkapiClientFactory(Vertx.vertx(), "http://localhost:" + freePort, reqTimeout);
    assertFalse(factory.getOkapiClient(tenant).healthy().get());
  }

  @Test
  public void testHealthy500(TestContext context) {
    Vertx.vertx().createHttpServer()
    .requestHandler(request -> request.response().setStatusCode(500).end())
    .listen(0)
    .compose(server -> {
      var factory = new OkapiClientFactory(Vertx.vertx(), "http://localhost:" + server.actualPort(), reqTimeout);
      return factory.getOkapiClient(tenant).health();
    })
    .onComplete(context.asyncAssertSuccess(health -> assertFalse(health)));
  }

  @Test
  public void testLoginNoUsername()  {
    logger.info("=== Test login w/ no password === ");

    assertThrows(IllegalArgumentException.class, () -> client.doLogin(null, "password"));
  }

  @Test
  public void testLoginNoPassword(TestContext context) {
    logger.info("=== Test login w/ no password === ");

    client.doLogin("admin", null).onComplete(context.asyncAssertFailure(res ->
            assertEquals("POST /authn/login returned status 400: Json content error", res.getMessage())
    ));
  }

  @Test
  public void testDeleteWithHeaders(TestContext context) {
    logger.info("=== Test delete w/ headers === ");

    int status = 204;

    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.set(X_ECHO_STATUS, String.valueOf(status));

    Async async = context.async();
    client.delete(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
        tenant,
        headers,
        resp -> {
          context.assertEquals(status, resp.statusCode());
          async.complete();
        },
        t -> context.fail(t));
  }

  @Test
  public void testPutWithHeaders(TestContext context) {
    logger.info("=== Test put w/ headers === ");

    int status = 204;

    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.set(X_ECHO_STATUS, String.valueOf(status));

    Async async = context.async();
    client.put(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
        tenant,
        headers,
        resp -> {
          context.assertEquals(status, resp.statusCode());
          async.complete();
        },
        t -> context.fail(t));
  }

  @Test
  public void testPostWithHeaders(TestContext context) {
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
        resp -> {
          context.assertEquals(status, resp.statusCode());
          context.assertEquals(obj.encode(), resp.bodyAsString());
          context.assertEquals(APPLICATION_JSON, resp.headers().get(HttpHeaders.CONTENT_TYPE));
          async.complete();
        },
        t -> {
          context.fail(t);
        });
  }

  @Test
  public void testGetWithHeaders(TestContext context) {
    logger.info("=== Test get w/ headers === ");

    int status = 404;

    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.set(X_ECHO_STATUS, String.valueOf(status));
    headers.set(HEADER_API_KEY, "foobarbaz");

    Async async = context.async();
    client.get(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
        tenant,
        headers,
        resp -> {
          context.assertEquals(status, resp.statusCode());
          context.assertNull(resp.headers().get(HEADER_API_KEY));
          async.complete();
        },
        t -> context.fail(t));
  }

  @Test
  public void testDeleteWithoutHeaders(TestContext context) {
    logger.info("=== Test delete w/o headers === ");

    Async async = context.async();
    client.delete(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
        tenant,
        resp -> {
          context.assertEquals(200, resp.statusCode());
          async.complete();
        },
        t -> context.fail(t));
  }

  @Test
  public void testPutWithoutHeaders(TestContext context) {
    logger.info("=== Test put w/o headers === ");

    Async async = context.async();
    client.put(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
        tenant,
        resp -> {
          context.assertEquals(200, resp.statusCode());
          async.complete();
        },
        t -> context.fail(t));
  }

  @Test
  public void testPostWithoutHeaders(TestContext context) {
    logger.info("=== Test post w/o headers === ");

    JsonObject obj = new JsonObject();
    obj.put("hello", "world");

    Async async = context.async();
    client.post(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
        tenant,
        obj.encode(),
        resp -> {
          context.assertEquals(200, resp.statusCode());
          context.assertEquals(obj.encode(), resp.bodyAsString());
          async.complete();
        },
        t -> context.fail(t));
  }

  @Test
  public void testGetWithoutHeaders(TestContext context) {
    logger.info("=== Test get w/o headers === ");

    Async async = context.async();
    client.get(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
        tenant,
        resp -> {
          context.assertEquals(200, resp.statusCode());
          async.complete();
        },
        t -> context.fail(t));
  }

  @Test
  public void testWrongTenantTokenHeaders(TestContext context) {
    var headers = MultiMap.caseInsensitiveMultiMap()
        .add(X_OKAPI_TENANT, "foo")
        .add(X_OKAPI_TOKEN, "foo");
    client.get(String.format("http://localhost:%s/echo", mockOkapi.okapiPort), "bar", headers)
    .onComplete(context.asyncAssertSuccess(response -> {
      assertThat(response.getHeader(X_OKAPI_TENANT), is("diku"));
      assertThat(response.getHeader(X_OKAPI_TOKEN), is(nullValue()));
    }));
  }

  @Test
  public void testTimeoutExceptionWhenResponseToGetRequestIsDelayed(TestContext context) {
    var headers = MultiMap.caseInsensitiveMultiMap();
    // Header used to tell the mock Okapi to delay the response
    headers.set(X_DURATION, Integer.toString(reqTimeout * 2));
    headers.set(X_ECHO_STATUS, "200");
    headers.set(HEADER_API_KEY, "foobarbaz");

    Async async = context.async();
    client.get(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
      tenant, headers,
      resp -> context.fail("shouldn't receive a response before the timeout occurs"),
      t -> {
        context.assertTrue(t instanceof java.util.concurrent.TimeoutException);
        async.complete();
      });
  }

  @Test
  public void testTimeoutExceptionWhenResponseToPostRequestIsDelayed(TestContext context) {
    var headers = MultiMap.caseInsensitiveMultiMap();
    // Header used to tell the mock Okapi to delay the response
    headers.set(X_DURATION, Integer.toString(reqTimeout * 2));
    headers.set(X_ECHO_STATUS, "200");
    headers.set(HEADER_API_KEY, "foobarbaz");

    Async async = context.async();
    client.post(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
      tenant, "", headers,
      resp -> context.fail("shouldn't receive a response before the timeout occurs"),
      t -> {
        context.assertTrue(t instanceof java.util.concurrent.TimeoutException);
        async.complete();
      });
  }

  @Test
  public void testTimeoutExceptionWhenResponseToDeleteRequestIsDelayed(TestContext context) {
    var headers = MultiMap.caseInsensitiveMultiMap();
    // Header used to tell the mock Okapi to delay the response
    headers.set(X_DURATION, Integer.toString(reqTimeout * 2));
    headers.set(X_ECHO_STATUS, "200");
    headers.set(HEADER_API_KEY, "foobarbaz");

    Async async = context.async();
    client.delete(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
      tenant, headers,
      resp -> context.fail("shouldn't receive a response before the timeout occurs"),
      t -> {
        context.assertTrue(t instanceof java.util.concurrent.TimeoutException);
        async.complete();
      });
  }

  @Test
  public void testTimeoutExceptionWhenResponseToPutRequestIsDelayed(TestContext context) {
    var headers = MultiMap.caseInsensitiveMultiMap();
    // Header used to tell the mock Okapi to delay the response
    headers.set(X_DURATION, Integer.toString(reqTimeout * 2));
    headers.set(X_ECHO_STATUS, "200");
    headers.set(HEADER_API_KEY, "foobarbaz");

    Async async = context.async();
    client.put(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
      tenant, headers,
      resp -> context.fail("shouldn't receive a response before the timeout occurs"),
      t -> {
        context.assertTrue(t instanceof java.util.concurrent.TimeoutException);
        async.complete();
      });
  }

  @Test
  public void testTimeoutExceptionWhenSetDelay(TestContext context) {
    assertThat(mockOkapi.getDelay(), is(0L));
    mockOkapi.setDelay(reqTimeout * 2);
    assertThat(mockOkapi.getDelay(), is(reqTimeout * 2L));
    long start = System.currentTimeMillis();

    Async async = context.async();
    client.get(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
      tenant, null,
      resp -> context.fail("shouldn't receive a response before the timeout occurs"),
      t -> context.verify(verify -> {
        assertThat(t, instanceOf(TimeoutException.class));
        assertThat(System.currentTimeMillis()-start, greaterThanOrEqualTo((long) reqTimeout));
        async.complete();
      }));
  }

  @Test
  public void testNoTimeoutWhenSetShortDelay(TestContext context) {
    mockOkapi.setDelay(reqTimeout / 2);

    Async async = context.async();
    client.get(String.format("http://localhost:%s/echo", mockOkapi.okapiPort),
      tenant, null,
      resp -> async.complete(),
      t -> context.fail(t));
  }

  @Test
  public void testConstructorWithSecondaryTenantHeader() {
    logger.info("=== Test secondary tenant constructor ===");
    OkapiClient secondaryClient = new OkapiClient(client, secondaryTenant);
    assertEquals(secondaryClient.secondaryTenantId, secondaryClient.defaultHeaders.get(X_OKAPI_TENANT));
  }

  @Test
  public void testConstructorWithEmptySecondaryTenantHeader() {
    logger.info("=== Test empty secondary tenant constructor ===");
    OkapiClient secondaryClient = new OkapiClient(client, "");
    assertEquals(secondaryClient.tenant, secondaryClient.defaultHeaders.get(X_OKAPI_TENANT));
  }

  @Test
  public void testConstructorWithSecondaryTenantHeaderVertx() {
    logger.info("=== Test secondary tenant with vertx constructor ===");
    int freePort = TestUtils.getPort();
    OkapiClient secondaryClient = new OkapiClient(Vertx.vertx(), "http://localhost:" + freePort, tenant, secondaryTenant, reqTimeout);
    assertEquals(secondaryClient.secondaryTenantId, secondaryClient.defaultHeaders.get(X_OKAPI_TENANT));
  }

  @Test
  public void testConstructorWithEmptySecondaryTenantHeaderVertx() {
    logger.info("=== Test empty secondary tenant with vertx constructor ===");
    int freePort = TestUtils.getPort();
    OkapiClient secondaryClient = new OkapiClient(Vertx.vertx(), "http://localhost:" + freePort, tenant, "", reqTimeout);
    assertEquals(secondaryClient.tenant, secondaryClient.defaultHeaders.get(X_OKAPI_TENANT));
  }

  @Test
  public void testConstructorForTlsWithNullTrustOptions() throws IllegalAccessException {
    logger.info("=== Test tls constructor with null trustOptions ===");
    int freePort = TestUtils.getPort();
    OkapiClient tlsClient = new OkapiClient(Vertx.vertx(), "http://localhost:" + freePort, tenant, reqTimeout, null);
    WebClientOptions options = (WebClientOptions) FieldUtils.readDeclaredField(tlsClient.client, "options", true);

    assertTrue(options.isSsl());
    assertNull(options.getTrustOptions());
  }

  @Test
  public void testConstructorForTlsWithTrustOptionsPopulated() throws IllegalAccessException {
    logger.info("=== Test tls constructor with trustOptions populated ===");
    int freePort = TestUtils.getPort();
    KeyStoreOptions trustOptions = new KeyStoreOptions()
      .setType("JKS")
      .setPath("some_path")
      .setPassword("some_password");
    OkapiClient tlsClient = new OkapiClient(Vertx.vertx(), "http://localhost:" + freePort, tenant, reqTimeout, trustOptions);
    WebClientOptions options = (WebClientOptions) FieldUtils.readDeclaredField(tlsClient.client, "options", true);

    assertTrue(options.isSsl());
    assertNotNull(options.getTrustOptions());
  }
}
