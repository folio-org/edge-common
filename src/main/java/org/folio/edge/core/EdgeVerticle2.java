package org.folio.edge.core;

import static org.folio.edge.core.Constants.DEFAULT_SECURE_STORE_TYPE;
import static org.folio.edge.core.Constants.PROP_SECURE_STORE_TYPE;
import static org.folio.edge.core.Constants.SYS_API_KEY_SOURCES;
import static org.folio.edge.core.Constants.SYS_LOG_LEVEL;
import static org.folio.edge.core.Constants.SYS_NULL_TOKEN_CACHE_TTL_MS;
import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_PORT;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.SYS_RESPONSE_COMPRESSION;
import static org.folio.edge.core.Constants.SYS_SECURE_STORE_PROP_FILE;
import static org.folio.edge.core.Constants.SYS_SECURE_STORE_TYPE;
import static org.folio.edge.core.Constants.SYS_TOKEN_CACHE_CAPACITY;
import static org.folio.edge.core.Constants.SYS_TOKEN_CACHE_TTL_MS;
import static org.folio.edge.core.Constants.TEXT_PLAIN;

import java.util.Properties;
import java.util.regex.Pattern;

import io.vertx.core.http.HttpServerOptions;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.folio.edge.core.cache.TokenCache;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.security.SecureStoreFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public abstract class EdgeVerticle2 extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger(EdgeVerticle2.class);

  private static Pattern isURL = Pattern.compile("(?i)^http[s]?://.*");

  protected SecureStore secureStore;

  @Override
  public void start(Promise<Void> promise) {
    JsonObject jo = Constants.DEFAULT_DEPLOYMENT_OPTIONS.copy();
    config().mergeIn(jo.mergeIn(config()));

    final int port = config().getInteger(SYS_PORT);
    logger.info("Using port: " + port);

    final String logLvl = config().getString(SYS_LOG_LEVEL);
    Configurator.setRootLevel(Level.toLevel(logLvl));
    logger.info("Using log level: " + logLvl);

    logger.info("Using okapi URL: " + config().getString(SYS_OKAPI_URL));
    logger.info("Using API key sources: " + config().getString(SYS_API_KEY_SOURCES));

    final long cacheTtlMs = config().getLong(SYS_TOKEN_CACHE_TTL_MS);
    logger.info("Using token cache TTL (ms): " + cacheTtlMs);

    final long failureCacheTtlMs = config().getLong(SYS_NULL_TOKEN_CACHE_TTL_MS);
    logger.info("Using token cache TTL (ms): " + failureCacheTtlMs);

    final int cacheCapacity = config().getInteger(SYS_TOKEN_CACHE_CAPACITY);
    logger.info("Using token cache capacity: " + cacheCapacity);

    logger.info("Using request timeout (ms): " + config().getLong(SYS_REQUEST_TIMEOUT_MS));

    // initialize the TokenCache
    TokenCache.initialize(cacheTtlMs, failureCacheTtlMs, cacheCapacity);

    secureStore = initializeSecureStore(config().getString(SYS_SECURE_STORE_PROP_FILE));

    // initialize response compression
    final boolean isCompressionSupported = config().getBoolean(SYS_RESPONSE_COMPRESSION);
    logger.info("Response compression enabled: " + isCompressionSupported);
    final HttpServerOptions serverOptions = new HttpServerOptions();
    serverOptions.setCompressionSupported(isCompressionSupported);

    final HttpServer server = getVertx().createHttpServer(serverOptions);

    final Router router = defineRoutes();

    server.requestHandler(router).listen(port)
        .onSuccess(res -> promise.complete())
        .onFailure(promise::fail);
  }

  public abstract Router defineRoutes();

  protected SecureStore initializeSecureStore(String secureStorePropFile) {
    Properties secureStoreProps = EdgeVerticle.getProperties(secureStorePropFile);

    // Order of precedence: system property, properties file, default
    String type = config().getString(SYS_SECURE_STORE_TYPE,
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
