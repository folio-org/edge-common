package org.folio.edge.core.cache;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.folio.edge.core.cache.Cache.CacheValue;
import org.junit.Before;
import org.junit.Test;

public class CacheTest {

  private static final Logger logger = Logger.getLogger(CacheTest.class);

  final int cap = 50;
  final long ttl = 3000;
  final long nullValueTtl = 1000;

  private Cache<Long> cache;

  private final String key = "someKey";

  @Before
  public void setUp() throws Exception {
    cache = new Cache.Builder<Long>()
      .withCapacity(cap)
      .withTTL(ttl)
      .withNullValueTTL(nullValueTtl)
      .build();
  }

  @Test
  public void testEmpty() throws Exception {
    logger.info("=== Test that a new cache is empty... ===");

    // empty cache...
    assertNull(cache.get(key));
  }

  @Test
  public void testGetPutGet() throws Exception {
    logger.info("=== Test basic functionality (Get, Put, Get)... ===");

    // empty cache...
    assertNull(cache.get(key));

    // basic functionality
    cache.put(key, 1L);
    assertEquals(1L, cache.get(key).longValue());
  }

  @Test
  public void testNoOverwrite() throws Exception {
    logger.info("=== Test entries aren't overwritten... ===");

    // make sure we don't overwrite the cached value
    Long val = 1L;

    cache.put(key, val);
    assertEquals(val.longValue(), cache.get(key).longValue());

    for (int i = 0; i < 100; i++) {
      cache.put(key, ++val);
      assertEquals(1L, cache.get(key).longValue());
    }

    // should expire very soon, if not already.
    await().with()
      .pollInterval(20, TimeUnit.MILLISECONDS)
      .atMost(ttl + 100, TimeUnit.MILLISECONDS)
      .until(() -> cache.get(key) == null);
  }

  @Test
  public void testPruneExpires() throws Exception {
    logger.info("=== Test pruning of expired entries... ===");

    CacheValue<Long> cached = cache.put(key + 0, 0L);
    await().with()
      .pollInterval(20, TimeUnit.MILLISECONDS)
      .atMost(ttl + 100, TimeUnit.MILLISECONDS)
      .until(() -> cached.expired());

    // load capacity + 1 entries triggering eviction of expired
    for (Long i = 1L; i <= cap; i++) {
      cache.put(key + i, i);
    }

    // should be evicted as it's expired
    assertNull(cache.get(key + 0));

    // should still be cached
    for (Long i = 1L; i <= cap; i++) {
      assertEquals(i.longValue(), cache.get(key + i).longValue());
    }
  }

  @Test
  public void testPruneNoExpires() throws Exception {
    logger.info("=== Test pruning of unexpired entries... ===");

    // load capacity + 1 entries triggering eviction of the first
    for (Long i = 0L; i <= cap; i++) {
      cache.put(key + i, i);
    }

    // should be evicted as it's the oldest
    assertNull(cache.get(key + 0));

    // should still be cached
    for (Long i = 1L; i <= cap; i++) {
      assertEquals(i.longValue(), cache.get(key + i).longValue());
    }
  }

  @Test
  public void testWithString() throws Exception {
    final String val = "someValue";

    Cache<String> cache = new Cache.Builder<String>()
      .withCapacity(cap)
      .withTTL(ttl)
      .withNullValueTTL(nullValueTtl)
      .build();

    // empty cache...
    assertNull(cache.get(key));

    // basic functionality
    CacheValue<String> cached = cache.put(key, val);
    assertEquals(val, cache.get(key));

    await().with()
      .pollInterval(20, TimeUnit.MILLISECONDS)
      .atMost(ttl + 100, TimeUnit.MILLISECONDS)
      .until(() -> cached.expired());

    // empty cache...
    assertNull(cache.get(key));
  }

  @Test
  public void testNullValueExpires() throws Exception {
    logger.info("=== Test expiration of null value entries... ===");

    String key = "nullValueKey";

    CacheValue<Long> cached = cache.put("nullValueKey", null);

    assertNull(cache.get(key));

    await().with()
      .pollInterval(20, TimeUnit.MILLISECONDS)
      .atMost(nullValueTtl + 100, TimeUnit.MILLISECONDS)
      .until(() -> cached.expired());

    assertNull(cache.get(key));
  }

  @Test(expected = IllegalStateException.class)
  public void testMissingTTL() {
    logger.info("=== Test construction w/o TTL... ===");

    new Cache.Builder<String>()
      .withCapacity(cap)
      .withNullValueTTL(nullValueTtl)
      .build();
  }

  @Test(expected = IllegalStateException.class)
  public void testMissingNullValueTTL() {
    logger.info("=== Test construction w/o Null Value TTL... ===");

    new Cache.Builder<String>()
      .withCapacity(cap)
      .withTTL(ttl)
      .build();
  }

  @Test(expected = IllegalStateException.class)
  public void testMissingCapacity() {
    logger.info("=== Test construction w/o Capacity... ===");

    new Cache.Builder<String>()
      .withTTL(ttl)
      .withNullValueTTL(nullValueTtl)
      .build();
  }
}
