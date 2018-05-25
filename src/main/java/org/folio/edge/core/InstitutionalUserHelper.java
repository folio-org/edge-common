package org.folio.edge.core;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;

import org.apache.log4j.Logger;
import org.folio.edge.core.cache.TokenCache;
import org.folio.edge.core.cache.TokenCache.NotInitializedException;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.utils.OkapiClient;

public class InstitutionalUserHelper {
  private static final Logger logger = Logger.getLogger(InstitutionalUserHelper.class);

  private final SecureStore secureStore;

  public InstitutionalUserHelper(SecureStore secureStore) {
    this.secureStore = secureStore;
  }

  public String getTenant(String apiKey) {
    String tenant = null;
    try {
      tenant = new String(Base64.getUrlDecoder().decode(apiKey.getBytes()));
      logger.info(String.format("API Key: %s, Tenant: %s", apiKey, tenant));

    } catch (Exception e) {
      logger.error(String.format("Failed to parse API Key %s", apiKey), e);
    }
    return tenant;
  }

  public CompletableFuture<String> getToken(OkapiClient client, String tenant, String username) {
    CompletableFuture<String> future = new CompletableFuture<>();

    String token = null;
    try {
      TokenCache cache = TokenCache.getInstance();
      token = cache.get(tenant, username);
    } catch (NotInitializedException e) {
      logger.warn("Failed to access TokenCache", e);
    }

    if (token != null) {
      logger.info("Using cached token");
      future.complete(token);
    } else {
      String password = secureStore.get(tenant, username);

      CompletableFuture<String> loginFuture = client.login(username, password);

      if (loginFuture.isCompletedExceptionally()) {
        try {
          loginFuture.get();
        } catch (Exception e) {
          logger.error("Login Failed", e);
          future.completeExceptionally(e);
        }
      } else {
        loginFuture.thenAccept(t -> {
          try {
            TokenCache.getInstance().put(tenant, username, t);
          } catch (NotInitializedException e) {
            logger.warn("Failed to cache token", e);
          }
          future.complete(t);
        });
      }
    }

    return future;
  }
}
