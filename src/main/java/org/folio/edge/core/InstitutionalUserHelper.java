package org.folio.edge.core;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.folio.edge.core.cache.TokenCache;
import org.folio.edge.core.cache.TokenCache.NotInitializedException;
import org.folio.edge.core.model.ClientInfo;
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

  public static ClientInfo parseApiKey(String apiKey) throws MalformedApiKeyException {
    ClientInfo ret = null;
    try {
      String decoded = new String(Base64.getUrlDecoder().decode(apiKey.getBytes()));
      String[] parts = DELIM.split(decoded);

      ret = new ClientInfo(parts[0], parts[1], parts[2]);

      logger.info(String.format("API Key: %s, Tenant: %s Username: %s", apiKey, ret.tenantId, ret.username));

    } catch (Exception e) {
      logger.error(String.format("Failed to parse API Key %s", apiKey), e);
      throw new MalformedApiKeyException(e);
    }

    if (ret.tenantId == null) {
      throw new MalformedApiKeyException("Null Tenant");
    }
    return ret;
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

  public static class MalformedApiKeyException extends Exception {

    private static final long serialVersionUID = 7852873967223950947L;

    public MalformedApiKeyException(Throwable t) {
      super(t);
    }

    public MalformedApiKeyException(String msg) {
      super(msg);
    }
  }

}
