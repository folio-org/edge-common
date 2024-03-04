package org.folio.edge.core.security;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EphemeralStore extends SecureStore {

  public static final String TYPE = "Ephemeral";

  public static final String PROP_TENANTS = "tenants";

  // split on comma, ignoring surrounding whitespace
  public static final Pattern COMMA = Pattern.compile("\\s*[,]\\s*");

  protected static final Logger logger = LogManager.getLogger(EphemeralStore.class);
  protected final Map<String, String> store = new ConcurrentHashMap<>();

  public EphemeralStore(Properties properties) {
    super(properties);
    logger.debug("Initializing...");

    if (properties != null) {
      String tenants = properties.getProperty(PROP_TENANTS);
      if (tenants != null) {
        for (String tenant : COMMA.split(tenants)) {
          String record = properties.getProperty(tenant);
          if (record != null) {
            String[] credentials = COMMA.split(record);
            String user = credentials[0];
            String password = credentials.length > 1 ? credentials[1] : "";
            put(tenant, user, password);
          } else {
            logger.error("Error extracting user/password for tenant: {}", tenant);
          }
        }
      }
    }

    if (store.isEmpty()) {
      logger.warn("Attention: No credentials were found/loaded");
    }
  }

  @Override
  public String get(String clientId, String tenant, String username) throws NotFoundException {
    // NOTE: ignore clientId
    String key = getKey(tenant, username);
    String ret = store.get(key);
    if(ret == null) {
      throw new NotFoundException("Nothing associated w/ key: " + key);
    }
    return ret;
  }

  private void put(String tenant, String username, String value) {
    store.put(getKey(tenant, username), value);
  }

  public String getKey(String tenant, String username) {
    return String.format("%s_%s", tenant, username);
  }
}
