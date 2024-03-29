package org.folio.edge.core;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import org.folio.edge.core.model.ClientInfo;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.utils.ApiKeyUtils;
import org.folio.edge.core.utils.ApiKeyUtils.MalformedApiKeyException;
import org.folio.edge.core.utils.OkapiClient;

public class InstitutionalUserHelper {
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

  public Future<String> fetchToken(OkapiClient client, String clientId, String tenant, String username) {
    return client.loginWithSupplier(username,
            () -> secureStore.get(getVertx(), clientId, tenant, username));
  }

  private Vertx getVertx() {
    Context context = Vertx.currentContext();

    if (context != null) {
      return context.owner();
    }

    return Vertx.vertx();
  }
}
