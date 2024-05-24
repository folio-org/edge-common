package org.folio.edge.core.utils;

import static org.folio.edge.core.Constants.SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PASSWORD;
import static org.folio.edge.core.Constants.SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_LOCATION;
import static org.folio.edge.core.Constants.SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_TYPE;
import static org.folio.edge.core.Constants.SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEY_ALIAS;
import static org.folio.edge.core.Constants.SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEY_PASSWORD;

import com.amazonaws.util.StringUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.core.net.NetServerOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SslConfigurationUtil {
  private static final Logger logger = LogManager.getLogger(SslConfigurationUtil.class);

  private SslConfigurationUtil() {}

  public static void configureSslServerOptionsIfEnabled(JsonObject config, NetServerOptions serverOptions) {
    final String keystoreType = config.getString(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_TYPE);
    final boolean isSslEnabled = !StringUtils.isNullOrEmpty(keystoreType);
    if (isSslEnabled) {
      logger.info("Enabling Vertx Http Server with TLS/SSL configuration...");
      serverOptions.setSsl(true);
      logger.info("Using {} keystore type for SSL/TLS", keystoreType);
      String keystoreLocation = config.getString(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_LOCATION);
      if (StringUtils.isNullOrEmpty(keystoreLocation)) {
        throw new IllegalStateException("'SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_LOCATION' system param must be specified");
      }
      String keystorePassword = config.getString(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PASSWORD);
      if (StringUtils.isNullOrEmpty(keystorePassword)) {
        throw new IllegalStateException("'SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PASSWORD' system param must be specified");
      }
      String keyAlias = config.getString(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEY_ALIAS);
      String keyAliasPassword = config.getString(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEY_PASSWORD);

      serverOptions.setKeyCertOptions(new KeyStoreOptions()
        .setType(keystoreType)
        .setPath(keystoreLocation)
        .setPassword(keystorePassword)
        .setAlias(keyAlias)
        .setAliasPassword(keyAliasPassword));
    }
  }
}
