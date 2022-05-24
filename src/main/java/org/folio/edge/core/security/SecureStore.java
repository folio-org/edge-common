package org.folio.edge.core.security;

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
   * <p>In Vert.x based code use {@link #get(Vertx, String, String, String)} to avoid blocking the event loop.
   */
  public abstract String get(String clientId, String tenant, String username) throws NotFoundException;

  /**
   * Fetch a value from the store using a thread from Vertx' worker pool to avoid blocking the event loop.
   */
  public Future<String> get(Vertx vertx, String clientId, String tenant, String username) {
    return vertx.executeBlocking(run -> {
      try {
        run.tryComplete(get(clientId, tenant, username));
      } catch (Exception e) {
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
