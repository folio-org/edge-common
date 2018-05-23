package org.folio.edge.core.security;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class EphemeralStore extends SecureStore {

  public static final String TYPE = "Ephemeral";

  public static final String PROP_TENANTS = "tenants";

  // split on comma, ignoring surrounding whitespace
  public static final Pattern COMMA = Pattern.compile("\\s*[,]\\s*");

  protected static final Logger logger = Logger.getLogger(EphemeralStore.class);
  protected final Map<String, String> store = new ConcurrentHashMap<>();

  public EphemeralStore(Properties properties) {
    super(properties);
    logger.info("Initializing...");

    if (properties != null) {
      String tenants = properties.getProperty(PROP_TENANTS);
      if (tenants != null) {
        for (String tenant : COMMA.split(tenants)) {
          String pass = properties.getProperty(tenant);
          put(tenant, tenant, pass == null ? "" : pass);
        }
      }
    }

    if (store.isEmpty()) {
      logger.warn("Attention: No credentials were found/loaded");
    }
  }

  @Override
  public String get(String tenant, String username) {
    return store.get(getKey(tenant, username));
  }

  private void put(String tenant, String username, String value) {
    store.put(getKey(tenant, username), value);
  }

  public String getKey(String tenant, String username) {
    return String.format("%s_%s", tenant, username);
  }

  public static class NoCredentialsDefinedException extends RuntimeException {

    private static final long serialVersionUID = 1811051169841565668L;

    public NoCredentialsDefinedException() {
      super("No tenants/credentials defined");
    }
  }
}
