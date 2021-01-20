package org.folio.edge.core.security;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SecureStoreFactory {

  protected static final Logger logger = LogManager.getLogger(SecureStoreFactory.class);

  private SecureStoreFactory() {

  }

  public static SecureStore getSecureStore(String type, Properties props) {
    SecureStore ret;

    if (type == null)
      type = "";

    switch (type) {
    case VaultStore.TYPE:
      ret = new VaultStore(props);
      break;
    case AwsParamStore.TYPE:
      ret = new AwsParamStore(props);
      break;
    case EphemeralStore.TYPE:
    default:
      ret = new EphemeralStore(props);
    }

    logger.info(String.format("type: %s, class: %s", type, ret.getClass().getName()));
    return ret;
  }

}
