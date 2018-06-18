package org.folio.edge.core.cache;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.folio.edge.core.cache.Cache.CacheValue;
import org.junit.Before;
import org.junit.Test;

public class TokenCacheTest {

  private static final Logger logger = Logger.getLogger(TokenCacheTest.class);

  final int cap = 50;
  final long ttl = 3000;
  final long nullValueTtl = 1000;

  private final String tenant = "diku";
  private final String user = "diku";
  private final String clientId = "abc123";
  private final String val = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJkaWt1IiwidXNlcl9pZCI6Ijk3ZTNmYzVmLTVjMzMtNGY2Ny1hZmRiLWEzYjI5YTVhYWZjOCIsInRlbmFudCI6ImRpa3UifQ.uda9KgBn82jCR3FXd73CnkmfDDk3OBQI0bjrJ5L7oJ8fS_7-TDNj7UKiFl-YxnqwFGHGACprsG5Bp7kkG8ArZA";

  @Before
  public void setUp() throws Exception {
    // initialize singleton cache
    TokenCache.initialize(ttl, nullValueTtl, cap);
  }

  @Test
  public void testReinitialize() throws Exception {
    logger.info("=== Test Reinitialize... ===");
    final CacheValue<String> cached = TokenCache.getInstance().put(clientId, tenant, user, val);

    await().with()
      .pollInterval(20, TimeUnit.MILLISECONDS)
      .atMost(ttl + 100, TimeUnit.MILLISECONDS)
      .until(() -> cached.expired());

    TokenCache.initialize(ttl * 2, nullValueTtl, cap);
    final CacheValue<String> cached2 = TokenCache.getInstance().put(clientId, tenant, user, val);

    await().with()
      .pollInterval(20, TimeUnit.MILLISECONDS)
      .atLeast(ttl, TimeUnit.MILLISECONDS)
      .atMost(ttl * 2 + 100, TimeUnit.MILLISECONDS)
      .until(() -> cached2.expired());
  }

  @Test
  public void testEmpty() throws Exception {
    logger.info("=== Test that a new cache is empty... ===");

    // empty cache...
    assertNull(TokenCache.getInstance().get(clientId, tenant, user));
  }

  @Test
  public void testGetPutGet() throws Exception {
    logger.info("=== Test basic functionality (Get, Put, Get)... ===");

    TokenCache cache = TokenCache.getInstance();

    // empty cache...
    assertNull(cache.get(clientId, tenant, user));

    // basic functionality
    cache.put(clientId, tenant, user, val);
    assertEquals(val, cache.get(clientId, tenant, user));
  }

  @Test
  public void testNoOverwrite() throws Exception {
    logger.info("=== Test entries aren't overwritten... ===");

    TokenCache cache = TokenCache.getInstance();
    String baseVal = "val";

    // make sure we don't overwrite the cached value
    cache.put(clientId, tenant, user, baseVal);
    assertEquals(baseVal, cache.get(clientId, tenant, user));

    for (int i = 0; i < 100; i++) {
      cache.put(clientId, tenant, user, baseVal + i);
      assertEquals(baseVal, cache.get(clientId, tenant, user));
    }

    // should expire very soon, if not already.
    await().with()
      .pollInterval(20, TimeUnit.MILLISECONDS)
      .atMost(ttl + 100, TimeUnit.MILLISECONDS)
      .until(() -> cache.get(clientId, tenant, user) == null);
  }

  @Test
  public void testPruneExpires() throws Exception {
    logger.info("=== Test pruning of expired entries... ===");

    TokenCache cache = TokenCache.getInstance();
    String baseVal = "val";

    CacheValue<String> cached = cache.put(clientId, tenant, user + 0, baseVal);
    await().with()
      .pollInterval(20, TimeUnit.MILLISECONDS)
      .atMost(ttl + 100, TimeUnit.MILLISECONDS)
      .until(() -> cached.expired());

    // load capacity + 1 entries triggering eviction of expired
    for (int i = 1; i <= cap; i++) {
      cache.put(clientId, tenant, user + i, baseVal + i);
    }

    // should be evicted as it's expired
    assertNull(cache.get(clientId, tenant, user + 0));

    // should still be cached
    for (int i = 1; i <= cap; i++) {
      assertEquals(baseVal + i, cache.get(clientId, tenant, user + i));
    }
  }

  @Test
  public void testPruneNoExpires() throws Exception {
    logger.info("=== Test pruning of unexpired entries... ===");

    TokenCache cache = TokenCache.getInstance();
    String baseVal = "val";

    // load capacity + 1 entries triggering eviction of the first
    for (int i = 0; i <= cap; i++) {
      cache.put(clientId, tenant, user + i, baseVal + i);
    }

    // should be evicted as it's the oldest
    assertNull(cache.get(clientId, tenant, user + 0));

    // should still be cached
    for (int i = 1; i <= cap; i++) {
      assertEquals(baseVal + i, cache.get(clientId, tenant, user + i));
    }
  }

  @Test
  public void testNullTokenExpires() throws Exception {
    logger.info("=== Test expiration of null token entries... ===");

    TokenCache cache = TokenCache.getInstance();

    CacheValue<String> cached = cache.put(clientId, tenant, user, null);

    assertNull(cache.get(clientId, tenant, user));

    await().with()
      .pollInterval(20, TimeUnit.MILLISECONDS)
      .atMost(nullValueTtl + 100, TimeUnit.MILLISECONDS)
      .until(() -> cached.expired());

    assertNull(cache.get(clientId, tenant, user));
  }
}
