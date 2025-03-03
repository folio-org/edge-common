package org.folio.edge.core.utils;

import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.FOLIO_CLIENT_TLS_ENABLED;
import static org.folio.edge.core.Constants.FOLIO_CLIENT_TLS_TRUSTSTOREPASSWORD;
import static org.folio.edge.core.Constants.FOLIO_CLIENT_TLS_TRUSTSTOREPATH;
import static org.folio.edge.core.Constants.FOLIO_CLIENT_TLS_TRUSTSTORETYPE;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.core.net.TrustOptions;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OkapiClientFactoryInitializer {
  private static final Logger logger = LogManager.getLogger(OkapiClientFactoryInitializer.class);

  private OkapiClientFactoryInitializer() {
  }

  public static OkapiClientFactory createInstance(Vertx vertx, JsonObject config) {
    String okapiUrl = config.getString(SYS_OKAPI_URL);
    Integer requestTimeout = config.getInteger(SYS_REQUEST_TIMEOUT_MS);
    boolean isSslEnabled = config.getBoolean(FOLIO_CLIENT_TLS_ENABLED);
    if (isSslEnabled) {
      logger.info("Creating OkapiClientFactory with Enhance HTTP Endpoint Security and TLS mode enabled");
      String truststoreType = config.getString(FOLIO_CLIENT_TLS_TRUSTSTORETYPE);
      String truststorePath = config.getString(FOLIO_CLIENT_TLS_TRUSTSTOREPATH);
      String truststorePassword = config.getString(FOLIO_CLIENT_TLS_TRUSTSTOREPASSWORD);
      if (StringUtils.isNotEmpty(truststoreType)
        && StringUtils.isNotEmpty(truststorePath)
        && StringUtils.isNotEmpty(truststorePassword)) {

        logger.info("Web client truststore options for type: {} are set, configuring Web Client with them", truststoreType);
        TrustOptions trustOptions = new KeyStoreOptions()
          .setType(truststoreType)
          .setPath(truststorePath)
          .setPassword(truststorePassword);
        return new OkapiClientFactory(vertx, okapiUrl, requestTimeout, trustOptions);
      } else {
        return new OkapiClientFactory(vertx, okapiUrl, requestTimeout, null);
      }
    } else {
      return new OkapiClientFactory(vertx, okapiUrl,  requestTimeout);
    }
  }
}
