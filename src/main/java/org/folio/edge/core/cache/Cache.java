package org.folio.edge.core.cache;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A general purpose cache storing entries with a set TTL,
 *
 * @param <T>
 */
public class Cache<T> {

  private static final Logger logger = LogManager.getLogger(Cache.class);

  private LinkedHashMap<String, CacheValue<T>> storage;
  private final long ttl;
  private final long nullValueTtl;
  private final int capacity;

  private Cache(long ttl, long nullValueTtl, int capacity) {
    this.ttl = ttl;
    this.nullValueTtl = nullValueTtl;
    this.capacity = capacity;
    storage = new LinkedHashMap<>(capacity);
  }

  public T get(String key) {
    CacheValue<T> cached = storage.get(key);

    if (cached != null) {
      if (cached.expired()) {
        storage.remove(key);
        return null;
      } else {
        return cached.value;
      }
    } else {
      return null;
    }
  }

  public CacheValue<T> put(String key, T value) {
    // Double-checked locking...
    CacheValue<T> cached = storage.get(key);
    if (cached == null || cached.expired()) {

      // lock to safeguard against multiple threads
      // trying to cache the same key at the same time
      synchronized (this) {
        cached = storage.get(key);
        if (cached == null || cached.expired()) {
          cached = new CacheValue<>(value, System.currentTimeMillis() + (value == null ? nullValueTtl : ttl));
          storage.put(key, cached);
        }
      }
    }

    if (storage.size() >= capacity) {
      prune();
    }

    return cached;
  }

  private void prune() {
    logger.info("Cache size before pruning: {}", storage.size());

    LinkedHashMap<String, CacheValue<T>> updated = new LinkedHashMap<>(capacity);
    for (String key : storage.keySet()) {
      CacheValue<T> val = storage.get(key);
      if (val != null && !val.expired()) {
        updated.put(key, val);
      } else {
        logger.info("Pruning expired cache entry: {}", key);
      }
    }

    if (updated.size() > capacity) {
      // this works because LinkedHashMap maintains order of insertion
      String key = updated.keySet().iterator().next();
      logger.info("Cache is above capacity and doesn't contain expired entries."
              + " Removing oldest entry ({})", key);
      updated.remove(key);
    }

    // atomic swap-in updated cache.
    storage = updated;

    logger.info("Cache size after pruning: {}", updated.size());
  }

  /**
   * A Generic, immutable cache entry.
   *
   * Expiration times are specified in ms since epoch.<br>
   * e.g. <code>System.currentTimeMills() + TTL</code>
   *
   * @param <T>
   *          The class/type of value being cached
   */
  public static final class CacheValue<T> {
    public final T value;
    public final long expires;

    public CacheValue(T value, long expires) {
      this.value = value;
      this.expires = expires;
    }

    public boolean expired() {
      return expires < System.currentTimeMillis();
    }
  }

  public static class Builder<T> {
    private Long ttl = null;
    private Long nullValueTtl = null;
    private Integer capacity = null;

    public Builder() {
      // nothing to do here...
    }

    public Builder<T> withTTL(long ttl) {
      this.ttl = ttl;
      return this;
    }

    public Builder<T> withNullValueTTL(long ttl) {
      this.nullValueTtl = ttl;
      return this;
    }

    public Builder<T> withCapacity(int capacity) {
      this.capacity = capacity;
      return this;
    }

    public Cache<T> build() {
      if (ttl == null) {
        throw new IllegalStateException("TTL must be specified");
      } else if (nullValueTtl == null) {
        throw new IllegalStateException("Null Value TTL must be specified");
      } else if (capacity == null) {
        throw new IllegalStateException("Capacity must be specified");
      }
      return new Cache<>(ttl, nullValueTtl, capacity);
    }
  }
}
