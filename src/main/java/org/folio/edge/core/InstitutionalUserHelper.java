package org.folio.edge.core;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.cache.TokenCache;
import org.folio.edge.core.cache.TokenCache.NotInitializedException;
import org.folio.edge.core.model.ClientInfo;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.utils.ApiKeyUtils;
import org.folio.edge.core.utils.ApiKeyUtils.MalformedApiKeyException;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.vertx.login.TokenClient;

public class InstitutionalUserHelper {
  private static final Logger logger = LogManager.getLogger(InstitutionalUserHelper.class);

  protected final SecureStore secureStore;

  public InstitutionalUserHelper(SecureStore secureStore) {
    this.secureStore = secureStore;
  }

  /**
   * @deprecated Use
   *             {@link org.folio.edge.core.utils.ApiKeyUtils#parseApiKey(String)}
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
    return fetchToken(client, clientId, tenant, username).toCompletionStage().toCompletableFuture();
  }

  public Future<String> fetchPassword(String clientId, String tenant, String username) {
    return secureStore.get(getVertx(), clientId, tenant, username);
  }

  public Future<String> fetchToken(OkapiClient client, String clientId, String tenant, String username) {
    try {
      TokenCache cache = TokenCache.getInstance();
      String token = cache.get(clientId, tenant, username);
      if (token != null) {
        logger.info("Using cached token");
        return Future.succeededFuture(token);
      }
    } catch (Exception e) {
      logger.warn("Failed to access TokenCache", e);
    }

    return secureStore.get(getVertx(), clientId, tenant, username)
        .compose(password -> client.doLogin(username, password))
        .onFailure(e -> logger.error("Exception during login", e))
        .onSuccess(token -> {
          try {
            TokenCache.getInstance().put(clientId, tenant, username, token);
          } catch (NotInitializedException e2) {
            logger.warn("Failed to cache token", e2);
          }
        });
  }

  private Vertx getVertx() {
    Context context = Vertx.currentContext();

    if (context != null) {
      return context.owner();
    }

    return Vertx.vertx();
  }
}
