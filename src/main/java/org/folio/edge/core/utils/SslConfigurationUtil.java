package org.folio.edge.core.utils;

import static org.folio.edge.core.Constants.SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PASSWORD;
import static org.folio.edge.core.Constants.SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PATH;
import static org.folio.edge.core.Constants.SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_TYPE;
import static org.folio.edge.core.Constants.SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEY_ALIAS;

import com.amazonaws.util.StringUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.core.net.NetServerOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

public class SslConfigurationUtil {
  private static final Logger logger = LogManager.getLogger(SslConfigurationUtil.class);

  private SslConfigurationUtil() {}

  public static void configureSslServerOptionsIfEnabled(JsonObject config, NetServerOptions serverOptions) {
    final boolean isSslEnabled = Objects.nonNull(config.getString(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_TYPE));
    if (isSslEnabled) {
      logger.info("Enabling Vertx Http Server with TLS/SSL configuration...");
      serverOptions.setSsl(true);
      String keystoreType = config.getString(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_TYPE);
      if (StringUtils.isNullOrEmpty(keystoreType)) {
        throw new IllegalStateException("'SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_TYPE' system param must be specified");
      }
      logger.info("Using {} keystore type for SSL/TLS", keystoreType);
      String keystorePath = config.getString(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PATH);
      if (StringUtils.isNullOrEmpty(keystorePath)) {
        throw new IllegalStateException("'SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PATH' system param must be specified");
      }
      String keystorePassword = config.getString(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PASSWORD);
      if (StringUtils.isNullOrEmpty(keystorePassword)) {
        throw new IllegalStateException("'SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEYSTORE_PASSWORD' system param must be specified");
      }
      String keyAlias = config.getString(SPRING_SSL_BUNDLE_JKS_WEB_SERVER_KEY_ALIAS);

      serverOptions.setKeyCertOptions(new KeyStoreOptions()
        .setType(keystoreType)
        .setPath(keystorePath)
        .setPassword(keystorePassword)
        .setAlias(keyAlias));
    }
  }
}
