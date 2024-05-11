package org.folio.edge.core;

import static org.folio.edge.core.Constants.BCFKS_PROVIDER;
import static org.folio.edge.core.Constants.BCFKS_TYPE;
import static org.folio.edge.core.Constants.SYS_KEYSTORE_PASSWORD;
import static org.folio.edge.core.Constants.SYS_KEYSTORE_PATH;
import static org.folio.edge.core.Constants.SYS_KEY_ALIAS;
import static org.folio.edge.core.Constants.SYS_PORT;
import static org.folio.edge.core.Constants.SYS_RESPONSE_COMPRESSION;
import static org.folio.edge.core.Constants.SYS_SSL_ENABLED;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
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
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

import java.security.Security;

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

        // initialize ssl
        final boolean isSslEnabled = config().getBoolean(SYS_SSL_ENABLED);
        logger.info("SSL enabled: {}", isSslEnabled);
        serverOptions.setSsl(isSslEnabled);
        if (isSslEnabled) {
          Security.addProvider(new BouncyCastleFipsProvider());
          serverOptions.setKeyCertOptions(new KeyStoreOptions()
            .setType(BCFKS_TYPE)
            .setProvider(BCFKS_PROVIDER)
            .setPath(config().getString(SYS_KEYSTORE_PATH))
            .setPassword(config().getString(SYS_KEYSTORE_PASSWORD))
            .setAlias(config().getString(SYS_KEY_ALIAS)));
        }

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
}
