package org.folio.edge.core;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.model.ClientInfo;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.utils.ApiKeyUtils;
import org.folio.edge.core.utils.ApiKeyUtils.MalformedApiKeyException;

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

  public Future<String> fetchPassword(String clientId, String tenant, String username) {
    return secureStore.get(getVertx(), clientId, tenant, username);
  }

  private Vertx getVertx() {
    Context context = Vertx.currentContext();

    if (context != null) {
      return context.owner();
    }

    return Vertx.vertx();
  }
}
