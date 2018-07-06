package org.folio.edge.core;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.folio.edge.core.cache.TokenCache;
import org.folio.edge.core.cache.TokenCache.NotInitializedException;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.security.SecureStore.NotFoundException;
import org.folio.edge.core.utils.OkapiClient;

public class InstitutionalUserHelper {
  private static final Logger logger = Logger.getLogger(InstitutionalUserHelper.class);

  public static final Pattern DELIM = Pattern.compile("_");

  protected final SecureStore secureStore;

  public InstitutionalUserHelper(SecureStore secureStore) {
    this.secureStore = secureStore;
  }

  public CompletableFuture<String> getToken(OkapiClient client, String clientId, String tenant, String username) {
    CompletableFuture<String> future = new CompletableFuture<>();

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
