package org.folio.edge.core;

import static org.folio.edge.core.Constants.SYS_KEYSTORE_PASSWORD;
import static org.folio.edge.core.Constants.SYS_KEYSTORE_PATH;
import static org.folio.edge.core.Constants.SYS_KEYSTORE_PROVIDER;
import static org.folio.edge.core.Constants.SYS_KEYSTORE_TYPE;
import static org.folio.edge.core.Constants.SYS_KEY_ALIAS;
import static org.folio.edge.core.Constants.SYS_KEY_ALIAS_PASSWORD;
import static org.folio.edge.core.Constants.SYS_PORT;
import static org.folio.edge.core.Constants.SYS_RESPONSE_COMPRESSION;
import static org.folio.edge.core.Constants.SYS_SSL_ENABLED;
import static org.folio.edge.core.Constants.TEXT_PLAIN;

import com.amazonaws.util.StringUtils;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Verticle for edge module which starts a HTTP service.
 */
public abstract class EdgeVerticleHttp extends EdgeVerticleCore {

  private static final Logger logger = LogManager.getLogger(EdgeVerticleHttp.class);

  @Override
  public void start(Promise<Void> promise) {
    Future.<Void>future(p -> super.start(p)).<Void>compose(res -> {
      final int port = config().getInteger(SYS_PORT);
      logger.info("Using port: {}", port);

      final HttpServerOptions serverOptions = new HttpServerOptions();

      // initialize response compression
      final boolean isCompressionSupported = config().getBoolean(SYS_RESPONSE_COMPRESSION);
      logger.info("Response compression enabled: {}", isCompressionSupported);
      serverOptions.setCompressionSupported(isCompressionSupported);

      // initialize tls/ssl configuration for web server
      configureSslIfEnabled(serverOptions);

      final HttpServer server = getVertx().createHttpServer(serverOptions);

      final Router router = defineRoutes();

      return server.requestHandler(router)
        .listen(port)
        .mapEmpty();
    }).onComplete(promise);
  }

  public abstract Router defineRoutes();

  protected void handleHealthCheck(RoutingContext ctx) {
    ctx.response()
      .setStatusCode(200)
      .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
      .end("\"OK\"");
  }

  private void configureSslIfEnabled(HttpServerOptions serverOptions) {
    final boolean isSslEnabled = config().getBoolean(SYS_SSL_ENABLED);
    if (isSslEnabled) {
      logger.info("Enabling Vertx Http Server with TLS/SSL configuration...");
      serverOptions.setSsl(true);
      String keystoreType = config().getString(SYS_KEYSTORE_TYPE);
      logger.info("Using {} keystore type for SSL/TLS", keystoreType);
      String keystoreProvider = config().getString(SYS_KEYSTORE_PROVIDER);
      logger.info("Using {} keystore provider for SSL/TLS", keystoreProvider);
      String keystorePath = config().getString(SYS_KEYSTORE_PATH);
      if (StringUtils.isNullOrEmpty(keystorePath)) {
        throw new IllegalStateException("'keystore_path' system param must be specified when ssl_enabled = true");
      }
      String keystorePassword = config().getString(SYS_KEYSTORE_PASSWORD);
      if (StringUtils.isNullOrEmpty(keystorePassword)) {
        throw new IllegalStateException("'keystore_password' system param must be specified when ssl_enabled = true");
      }
      String keyAlias = config().getString(SYS_KEY_ALIAS);
      String keyAliasPassword = config().getString(SYS_KEY_ALIAS_PASSWORD);

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
