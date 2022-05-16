package org.folio.edge.core.security;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.Properties;

public abstract class SecureStore {

  protected Properties properties;

  protected SecureStore(Properties properties) {
    this.properties = properties;
  }

  /**
   * Fetch a value from the store.
   *
   * <p>In Vert.x based code use {@code #lookup(String, String, String) to avoid blocking the event loop.
   */
  public abstract String get(String clientId, String tenant, String username) throws NotFoundException;

  public Future<String> lookup(String clientId, String tenant, String username) {
    Context context = Vertx.currentContext();
    if (context == null) {
      context = Vertx.vertx().getOrCreateContext();
    }
    return context.executeBlocking(run -> {
      try {
        run.tryComplete(get(clientId, tenant, username));
      } catch (Throwable e) {
        run.tryFail(e);
      }
    });
  }

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
