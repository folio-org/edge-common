package org.folio.edge.core.security;

import java.util.Properties;

public abstract class SecureStore {

  protected Properties properties;

  protected SecureStore(Properties properties) {
    this.properties = properties;
  }

  public abstract String get(String clientId, String tenant, String username);

}
