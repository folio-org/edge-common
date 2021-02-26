package org.folio.edge.core;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.folio.edge.core.cache.TokenCache;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.security.SecureStoreFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.folio.edge.core.Constants.*;
import static org.folio.edge.core.Constants.SYS_SECURE_STORE_PROP_FILE;

/**
 * Verticle for Edge module with no listeninger
 */
public class EdgeVerticleCore extends AbstractVerticle {
  private static final Logger logger = LogManager.getLogger(EdgeVerticleCore.class);

  protected SecureStore secureStore;

  private static final Pattern isURL = Pattern.compile("(?i)^http[s]?://.*");

  @Override
  public void start(Promise<Void> promise) {
    JsonObject jo = Constants.DEFAULT_DEPLOYMENT_OPTIONS.copy();
    config().mergeIn(jo.mergeIn(config()));

    final String logLvl = config().getString(SYS_LOG_LEVEL);
    Configurator.setRootLevel(Level.toLevel(logLvl));
    logger.info("Using log level: {}", logLvl);

    logger.info("Using okapi URL: {}", config().getString(SYS_OKAPI_URL));
    logger.info("Using API key sources: {}", config().getString(SYS_API_KEY_SOURCES));

    final long cacheTtlMs = config().getLong(SYS_TOKEN_CACHE_TTL_MS);
    logger.info("Using token cache TTL (ms): {}", cacheTtlMs);

    final long failureCacheTtlMs = config().getLong(SYS_NULL_TOKEN_CACHE_TTL_MS);
    logger.info("Using token cache TTL (ms): {}", failureCacheTtlMs);

    final int cacheCapacity = config().getInteger(SYS_TOKEN_CACHE_CAPACITY);
    logger.info("Using token cache capacity: {}", cacheCapacity);

    logger.info("Using request timeout (ms): {}", config().getLong(SYS_REQUEST_TIMEOUT_MS));

    // initialize the TokenCache
    TokenCache.initialize(cacheTtlMs, failureCacheTtlMs, cacheCapacity);

    initializeSecureStore(config().getString(SYS_SECURE_STORE_PROP_FILE))
      .onSuccess(res -> {
        secureStore = res;
        promise.complete();
      })
      .onFailure(promise::fail);
  }

  protected Future<SecureStore> initializeSecureStore(String secureStorePropFile) {
    return getProperties(secureStorePropFile).compose(secureStoreProps -> {
      // Order of precedence: system property, properties file, default
      String type = config().getString(SYS_SECURE_STORE_TYPE,
        secureStoreProps.getProperty(PROP_SECURE_STORE_TYPE, DEFAULT_SECURE_STORE_TYPE));

      return Future.succeededFuture(SecureStoreFactory.getSecureStore(type, secureStoreProps));
    });
  }

  static Future<Properties> getProperties(String secureStorePropFile) {
    Properties secureStoreProps = new Properties();

    if (secureStorePropFile != null) {
      URL url = null;
      try {
        if (isURL.matcher(secureStorePropFile).matches()) {
          url = new URL(secureStorePropFile);
        }

        try (
          InputStream in = url == null ? new FileInputStream(secureStorePropFile) : url.openStream()) {
          secureStoreProps.load(in);
          logger.info("Successfully loaded properties from: " +
            secureStorePropFile);
        }
      } catch (Exception e) {
        logger.warn(e.getMessage(), e);
        return Future.failedFuture("Failed to load secure store properties: " + e.getMessage());
      }
    } else {
      logger.warn("No secure store properties file specified.  Using defaults");
    }
    return Future.succeededFuture(secureStoreProps);
  }

}
