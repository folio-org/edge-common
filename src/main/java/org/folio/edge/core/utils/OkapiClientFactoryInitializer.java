package org.folio.edge.core.utils;

import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.SYS_WEB_CLIENT_KEY_ALIAS;
import static org.folio.edge.core.Constants.SYS_WEB_CLIENT_KEY_ALIAS_PASSWORD;
import static org.folio.edge.core.Constants.SYS_WEB_CLIENT_SSL_ENABLED;
import static org.folio.edge.core.Constants.SYS_WEB_CLIENT_TRUSTSTORE_PASSWORD;
import static org.folio.edge.core.Constants.SYS_WEB_CLIENT_TRUSTSTORE_PATH;
import static org.folio.edge.core.Constants.SYS_WEB_CLIENT_TRUSTSTORE_PROVIDER;
import static org.folio.edge.core.Constants.SYS_WEB_CLIENT_TRUSTSTORE_TYPE;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.core.net.TrustOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OkapiClientFactoryInitializer {
  private static final Logger logger = LogManager.getLogger(OkapiClientFactoryInitializer.class);

  private OkapiClientFactoryInitializer() {
  }

  public static OkapiClientFactory createInstance(Vertx vertx, JsonObject config) {
    String okapiUrl = config.getString(SYS_OKAPI_URL);
    Integer requestTimeout = config.getInteger(SYS_REQUEST_TIMEOUT_MS);
    boolean isSslEnabled = config.getBoolean(SYS_WEB_CLIENT_SSL_ENABLED);
    if (isSslEnabled) {
      logger.info("Creating OkapiClientFactory with Enhance HTTP Endpoint Security and TLS mode enabled");
      String truststoreType = config.getString(SYS_WEB_CLIENT_TRUSTSTORE_TYPE);
      String truststoreProvider = config.getString(SYS_WEB_CLIENT_TRUSTSTORE_PROVIDER);
      String truststorePath = config.getString(SYS_WEB_CLIENT_TRUSTSTORE_PATH);
      String truststorePassword = config.getString(SYS_WEB_CLIENT_TRUSTSTORE_PASSWORD);
      String keyAlias = config.getString(SYS_WEB_CLIENT_KEY_ALIAS);
      String keyAliasPassword = config.getString(SYS_WEB_CLIENT_KEY_ALIAS_PASSWORD);
      if (truststoreType != null && truststorePath != null && truststorePassword != null) {
        logger.info("Web client truststore options for type: {} are set, configuring Web Client with them", truststoreType);
        TrustOptions trustOptions = new KeyStoreOptions()
          .setType(truststoreType)
          .setProvider(truststoreProvider)
          .setPath(truststorePath)
          .setPassword(truststorePassword)
          .setAlias(keyAlias)
          .setAliasPassword(keyAliasPassword);
        return new OkapiClientFactory(vertx, okapiUrl, requestTimeout, trustOptions);
      } else {
        return new OkapiClientFactory(vertx, okapiUrl, requestTimeout, null);
      }
    } else {
      return new OkapiClientFactory(vertx, okapiUrl,  requestTimeout);
    }
  }
}
