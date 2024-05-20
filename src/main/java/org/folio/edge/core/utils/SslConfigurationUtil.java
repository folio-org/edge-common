package org.folio.edge.core.utils;

import static org.folio.edge.core.Constants.SYS_HTTP_SERVER_KEYSTORE_PASSWORD;
import static org.folio.edge.core.Constants.SYS_HTTP_SERVER_KEYSTORE_PATH;
import static org.folio.edge.core.Constants.SYS_HTTP_SERVER_KEYSTORE_PROVIDER;
import static org.folio.edge.core.Constants.SYS_HTTP_SERVER_KEYSTORE_TYPE;
import static org.folio.edge.core.Constants.SYS_HTTP_SERVER_KEY_ALIAS;
import static org.folio.edge.core.Constants.SYS_HTTP_SERVER_KEY_ALIAS_PASSWORD;
import static org.folio.edge.core.Constants.SYS_HTTP_SERVER_SSL_ENABLED;

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
    final boolean isSslEnabled = config.getBoolean(SYS_HTTP_SERVER_SSL_ENABLED);
    if (isSslEnabled) {
      logger.info("Enabling Vertx Http Server with TLS/SSL configuration...");
      serverOptions.setSsl(true);
      String keystoreType = config.getString(SYS_HTTP_SERVER_KEYSTORE_TYPE);
      if (StringUtils.isNullOrEmpty(keystoreType)) {
        throw new IllegalStateException("'keystore_type' system param must be specified when ssl_enabled = true");
      }
      logger.info("Using {} keystore type for SSL/TLS", keystoreType);
      String keystoreProvider = config.getString(SYS_HTTP_SERVER_KEYSTORE_PROVIDER);
      logger.info("Using {} keystore provider for SSL/TLS", keystoreProvider);
      String keystorePath = config.getString(SYS_HTTP_SERVER_KEYSTORE_PATH);
      if (StringUtils.isNullOrEmpty(keystorePath)) {
        throw new IllegalStateException("'keystore_path' system param must be specified when ssl_enabled = true");
      }
      String keystorePassword = config.getString(SYS_HTTP_SERVER_KEYSTORE_PASSWORD);
      if (StringUtils.isNullOrEmpty(keystorePassword)) {
        throw new IllegalStateException("'keystore_password' system param must be specified when ssl_enabled = true");
      }
      String keyAlias = config.getString(SYS_HTTP_SERVER_KEY_ALIAS);
      String keyAliasPassword = config.getString(SYS_HTTP_SERVER_KEY_ALIAS_PASSWORD);

      serverOptions.setKeyCertOptions(new KeyStoreOptions()
        .setType(keystoreType)
        .setProvider(keystoreProvider)
        .setPath(keystorePath)
        .setPassword(keystorePassword)
        .setAlias(keyAlias)
        .setAliasPassword(keyAliasPassword));
    }
  }
}
