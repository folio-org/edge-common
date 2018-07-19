package org.folio.edge.core;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.folio.edge.core.cache.TokenCache;
import org.folio.edge.core.cache.TokenCache.NotInitializedException;
import org.folio.edge.core.model.ClientInfo;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.security.SecureStore.NotFoundException;
import org.folio.edge.core.utils.ApiKeyUtils;
import org.folio.edge.core.utils.ApiKeyUtils.MalformedApiKeyException;
import org.folio.edge.core.utils.OkapiClient;

import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public class InstitutionalUserHelper {
  private static final Logger logger = Logger.getLogger(InstitutionalUserHelper.class);

  public static final Pattern DELIM = Pattern.compile("_");

  protected final SecureStore secureStore;

  public InstitutionalUserHelper(SecureStore secureStore) {
    this.secureStore = secureStore;
  }

  /**
   * @deprecated Use
   *             {@link #org.folio.edge.core.utils.ApiKeyUtils.parseApiKey(String)}
   *             instead
   * @param apiKey
   *          - The API key to parse
   * @return A ClientInfo object containing the information parsed from the
   *         provided key
   * @throws MalformedApiKeyException
   *           If there was a problem parsing the key
   */
  @Deprecated
  public static ClientInfo parseApiKey(String apiKey) throws MalformedApiKeyException {
    return ApiKeyUtils.parseApiKey(apiKey);
  }

  public CompletableFuture<String> getToken(OkapiClient client, String clientId, String tenant, String username) {
    VertxCompletableFuture<String> future = new VertxCompletableFuture<>(client.vertx);

    String token = null;
    try {
      TokenCache cache = TokenCache.getInstance();
      token = cache.get(clientId, tenant, username);
    } catch (NotInitializedException e) {
      logger.warn("Failed to access TokenCache", e);
    }

    if (token != null) {
      logger.info("Using cached token");
      future.complete(token);
    } else {
      String password;
      try {
        password = secureStore.get(clientId, tenant, username);
      } catch (NotFoundException e) {
        logger.error("Exception retreiving password", e);
        future.completeExceptionally(e);
        return future;
      }
      client.login(username, password).thenAcceptAsync(t -> {
        try {
          TokenCache.getInstance().put(clientId, tenant, username, t);
        } catch (NotInitializedException e) {
          logger.warn("Failed to cache token", e);
        }
        future.complete(t);
      }).exceptionally(t -> {
        logger.error("Exception during login", t);
        future.completeExceptionally(t);
        return null;
      });
    }
    return future;
  }
}
