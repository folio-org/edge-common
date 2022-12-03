package org.folio.edge.core.cache;

import org.folio.vertx.tokencache.TokenCache;

public class TokenCacheFactory {

    private TokenCacheFactory() { }

    static org.folio.vertx.tokencache.TokenCache instance;

    public static void initialize(int capacity) {
        instance = org.folio.vertx.tokencache.TokenCache.create(capacity);
    }

    public static TokenCache get() {
        return instance;
    }
}
