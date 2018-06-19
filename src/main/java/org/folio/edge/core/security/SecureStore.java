package org.folio.edge.core.security;

import java.util.Properties;

public abstract class SecureStore {

  protected Properties properties;

  protected SecureStore(Properties properties) {
    this.properties = properties;
  }

  public abstract String get(String clientId, String tenant, String username) throws NotFoundException;

  public static class NotFoundException extends Exception {

    private static final long serialVersionUID = 1586174011075039404L;

    public NotFoundException(String msg) {
      super(msg);
    }

    public NotFoundException(Throwable t) {
      super(t);
    }

  }

}
