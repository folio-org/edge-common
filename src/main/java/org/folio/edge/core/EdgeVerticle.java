package org.folio.edge.core;

import static org.folio.edge.core.Constants.DEFAULT_LOG_LEVEL;
import static org.folio.edge.core.Constants.DEFAULT_NULL_TOKEN_CACHE_TTL_MS;
import static org.folio.edge.core.Constants.DEFAULT_PORT;
import static org.folio.edge.core.Constants.DEFAULT_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.DEFAULT_SECURE_STORE_TYPE;
import static org.folio.edge.core.Constants.DEFAULT_TOKEN_CACHE_CAPACITY;
import static org.folio.edge.core.Constants.DEFAULT_TOKEN_CACHE_TTL_MS;
import static org.folio.edge.core.Constants.PROP_SECURE_STORE_TYPE;
import static org.folio.edge.core.Constants.SYS_API_KEY_SOURCES;
import static org.folio.edge.core.Constants.SYS_LOG_LEVEL;
import static org.folio.edge.core.Constants.SYS_NULL_TOKEN_CACHE_TTL_MS;
import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_PORT;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.SYS_SECURE_STORE_PROP_FILE;
import static org.folio.edge.core.Constants.SYS_SECURE_STORE_TYPE;
import static org.folio.edge.core.Constants.SYS_TOKEN_CACHE_CAPACITY;
import static org.folio.edge.core.Constants.SYS_TOKEN_CACHE_TTL_MS;
import static org.folio.edge.core.Constants.TEXT_PLAIN;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.folio.edge.core.cache.TokenCache;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.security.SecureStoreFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * Verticle for edge module.
 * Deprecated, use {@link EdgeVerticle2} instead.
 */
@Deprecated
public abstract class EdgeVerticle extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger(EdgeVerticle.class);

  private static Pattern isURL = Pattern.compile("(?i)^http[s]?://.*");

  public final int port;
  public final String okapiURL;
  public final String apiKeySources;
  public final long reqTimeoutMs;

  public final HttpServer server;
  public final SecureStore secureStore;
  public final Router router;

  protected EdgeVerticle() {
    super();

    final String logLvl = System.getProperty(SYS_LOG_LEVEL, DEFAULT_LOG_LEVEL);
    Configurator.setRootLevel(Level.toLevel(logLvl));
    logger.info("Using log level: " + logLvl);

    final String portStr = System.getProperty(SYS_PORT, DEFAULT_PORT);
    port = Integer.parseInt(portStr);
    logger.info("Using port: " + port);

    okapiURL = System.getProperty(SYS_OKAPI_URL);
    logger.info("Using okapi URL: " + okapiURL);

    apiKeySources = System.getProperty(SYS_API_KEY_SOURCES);
    logger.info("Using API key sources: " + apiKeySources);

    final String tokenCacheTtlMs = System.getProperty(SYS_TOKEN_CACHE_TTL_MS);
    final long cacheTtlMs = tokenCacheTtlMs != null ? Long.parseLong(tokenCacheTtlMs) : DEFAULT_TOKEN_CACHE_TTL_MS;
    logger.info("Using token cache TTL (ms): " + tokenCacheTtlMs);

    final String nullTokenCacheTtlMs = System.getProperty(SYS_NULL_TOKEN_CACHE_TTL_MS);
    final long failureCacheTtlMs = nullTokenCacheTtlMs != null ? Long.parseLong(nullTokenCacheTtlMs)
        : DEFAULT_NULL_TOKEN_CACHE_TTL_MS;
    logger.info("Using token cache TTL (ms): " + failureCacheTtlMs);

    final String tokenCacheCapacity = System.getProperty(SYS_TOKEN_CACHE_CAPACITY);
    final int cacheCapacity = tokenCacheCapacity != null ? Integer.parseInt(tokenCacheCapacity)
        : DEFAULT_TOKEN_CACHE_CAPACITY;
    logger.info("Using token cache capacity: " + tokenCacheCapacity);

    final String requestTimeout = System.getProperty(SYS_REQUEST_TIMEOUT_MS);
    reqTimeoutMs = requestTimeout != null ? Long.parseLong(requestTimeout)
        : DEFAULT_REQUEST_TIMEOUT_MS;
    logger.info("Using request timeout (ms): " + reqTimeoutMs);

    // initialize the TokenCache
    TokenCache.initialize(cacheTtlMs, failureCacheTtlMs, cacheCapacity);

    final String secureStorePropFile = System.getProperty(SYS_SECURE_STORE_PROP_FILE);
    secureStore = initializeSecureStore(secureStorePropFile);

    vertx = Vertx.vertx();
    server = vertx.createHttpServer();

    router = defineRoutes();
  }

  @Override
  public void start(Future<Void> future) {
    server.requestHandler(router::accept).listen(port, result -> {
      if (result.succeeded()) {
        future.complete();
      } else {
        future.fail(result.cause());
      }
    });
  }

  public abstract Router defineRoutes();

  protected SecureStore initializeSecureStore(String secureStorePropFile) {
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
        logger.warn("Failed to load secure store properties.", e);
      }
    } else {
      logger.warn("No secure store properties file specified.  Using defaults");
    }

    // Order of precedence: system property, properties file, default
    String type = System.getProperty(SYS_SECURE_STORE_TYPE,
        secureStoreProps.getProperty(PROP_SECURE_STORE_TYPE, DEFAULT_SECURE_STORE_TYPE));

    return SecureStoreFactory.getSecureStore(type, secureStoreProps);
  }

  protected void handleHealthCheck(RoutingContext ctx) {
    ctx.response()
      .setStatusCode(200)
      .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
      .end("\"OK\"");
  }
}
