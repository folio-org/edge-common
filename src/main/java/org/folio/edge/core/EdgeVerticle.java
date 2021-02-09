package org.folio.edge.core;

import static org.folio.edge.core.Constants.SYS_PORT;
import static org.folio.edge.core.Constants.SYS_RESPONSE_COMPRESSION;
import static org.folio.edge.core.Constants.TEXT_PLAIN;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Verticle for edge module.
 */
public abstract class EdgeVerticle extends EdgeVerticleCore {

  private static final Logger logger = LogManager.getLogger(EdgeVerticle.class);

  @Override
  public void start(Promise<Void> promise) {
    Promise<Void> promise1 = Promise.promise();
    super.start(promise1);
    Future<Void> f = promise1.future().compose(res -> {
        final int port = config().getInteger(SYS_PORT);
        logger.info("Using port: {}", port);

        // initialize response compression
        final boolean isCompressionSupported = config().getBoolean(SYS_RESPONSE_COMPRESSION);
        logger.info("Response compression enabled: {}", isCompressionSupported);
        final HttpServerOptions serverOptions = new HttpServerOptions();
        serverOptions.setCompressionSupported(isCompressionSupported);

        final HttpServer server = getVertx().createHttpServer(serverOptions);

        final Router router = defineRoutes();

        return server.requestHandler(router)
          .listen(port)
          .mapEmpty();
      });
    f.onComplete(promise);
  }

  public abstract Router defineRoutes();

  protected void handleHealthCheck(RoutingContext ctx) {
    ctx.response()
      .setStatusCode(200)
      .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
      .end("\"OK\"");
  }
}
