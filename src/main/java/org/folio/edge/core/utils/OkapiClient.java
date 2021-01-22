package org.folio.edge.core.utils;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.HEADER_API_KEY;
import static org.folio.edge.core.Constants.JSON_OR_TEXT;
import static org.folio.edge.core.Constants.X_OKAPI_TENANT;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;

import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OkapiClient {

  private static final Logger logger = LogManager.getLogger(OkapiClient.class);

  public final String okapiURL;
  public final WebClient client;
  public final String tenant;
  public final int reqTimeout;
  public final Vertx vertx;

  protected final MultiMap defaultHeaders = MultiMap.caseInsensitiveMultiMap();

  public OkapiClient(OkapiClient client) {
    this.vertx = client.vertx;
    this.reqTimeout = client.reqTimeout;
    this.tenant = client.tenant;
    this.okapiURL = client.okapiURL;
    this.client = client.client;
    this.setToken(client.getToken());
    initDefaultHeaders();
  }

  protected OkapiClient(Vertx vertx, String okapiURL, String tenant, int timeout) {
    this.vertx = vertx;
    this.reqTimeout = timeout;
    this.okapiURL = okapiURL;
    this.tenant = tenant;
    WebClientOptions options = new WebClientOptions().setKeepAlive(false).setTryUseCompression(true)
        .setIdleTimeoutUnit(TimeUnit.MILLISECONDS).setIdleTimeout(timeout);
    client = WebClient.create(vertx, options);
    initDefaultHeaders();
  }

  protected void initDefaultHeaders() {
    defaultHeaders.add(HttpHeaders.ACCEPT.toString(), JSON_OR_TEXT);
    defaultHeaders.add(HttpHeaders.CONTENT_TYPE.toString(), APPLICATION_JSON);
    defaultHeaders.add(X_OKAPI_TENANT, tenant);
  }

  public CompletableFuture<String> login(String username, String password) {
    return login(username, password, null);
  }

  public CompletableFuture<String> login(String username, String password, MultiMap headers) {
    VertxCompletableFuture<String> future = new VertxCompletableFuture<>(vertx);

    if (username == null || password == null) {
      future.complete(null);
      return future;
    }

    JsonObject payload = new JsonObject();
    payload.put("username", username);
    payload.put("password", password);

    post(
        okapiURL + "/authn/login",
        tenant,
        payload.encode(),
        combineHeadersWithDefaults(headers),
        response -> {
          if (response.statusCode() == 201) {
            logger.info("Successfully logged into FOLIO");
            String token = response.getHeader(X_OKAPI_TOKEN);
            setToken(token);
            future.complete(token);
          } else {
            logger.warn(String.format(
                "Failed to log into FOLIO: (%s) %s",
                response.statusCode(),
                response.bodyAsString()));
            future.complete(null);
          }
        },
        cause -> {
          logger.error("Exception during login: " + cause.getMessage());
          future.completeExceptionally(cause);
        });
    return future;
  }

  public CompletableFuture<Boolean> healthy() {
    VertxCompletableFuture<Boolean> future = new VertxCompletableFuture<>(vertx);
    get(
        okapiURL + "/_/proxy/health",
        tenant, response -> {
          int status = response.statusCode();
          if (status == 200) {
            future.complete(true);
          } else {
            logger.error(String.format("OKAPI is unhealthy! status: %s body: %s", status, response.bodyAsString()));
            future.complete(false);
          }
        },
        t -> {
          logger.error("Exception checking OKAPI's health: " + t.getMessage());
          future.complete(false);
        });
    return future;
  }

  public String getToken() {
    return defaultHeaders.get(X_OKAPI_TOKEN);
  }

  public void setToken(String token) {
    defaultHeaders.set(X_OKAPI_TOKEN, token == null ? "" : token);
  }

  public void post(String url, String tenant, String payload, Handler<HttpResponse<Buffer>> responseHandler,
      Handler<Throwable> exceptionHandler) {
    post(url, tenant, payload, null, responseHandler, exceptionHandler);
  }


  public void post(String url, String tenant, String payload, MultiMap headers,
                   Handler<HttpResponse<Buffer>> responseHandler,
                   Handler<Throwable> exceptionHandler) {
    post(url, tenant, payload, headers)
        .onSuccess(res -> responseHandler.handle(res))
        .onFailure(cause -> exceptionHandler.handle(cause));
  }

  public Future<HttpResponse<Buffer>> post(String url, String tenant, String payload, MultiMap headers) {
    HttpRequest<Buffer> request = client.postAbs(url);

    if (headers != null) {
      request.headers().setAll(combineHeadersWithDefaults(headers));
    } else {
      request.headers().setAll(defaultHeaders);
    }

    if (logger.isTraceEnabled()) {
      logger.trace(String.format("POST %s Request: %s tenant: %s token: %s", url, payload, tenant,
          request.headers().get(X_OKAPI_TOKEN)));
    }

    // TODO   .setTimeout(reqTimeout);

    if (payload != null) {
      return request.sendBuffer(Buffer.buffer(payload));
    } else {
      return request.send();
    }
  }

  public void delete(String url, String tenant, Handler<HttpResponse<Buffer>> responseHandler,
      Handler<Throwable> exceptionHandler) {
    delete(url, tenant, null, responseHandler, exceptionHandler);
  }

  public void delete(String url, String tenant, MultiMap headers,
                     Handler<HttpResponse<Buffer>> responseHandler,
                     Handler<Throwable> exceptionHandler) {
    delete(url, tenant, headers)
        .onSuccess(res -> responseHandler.handle(res))
        .onFailure(cause -> exceptionHandler.handle(cause));
  }

  public Future<HttpResponse<Buffer>> delete(String url, String tenant, MultiMap headers) {
    HttpRequest<Buffer> request = client.deleteAbs(url);

    if (headers != null) {
      request.headers().setAll(combineHeadersWithDefaults(headers));
    } else {
      request.headers().setAll(defaultHeaders);
    }

    logger.info(String.format("DELETE %s tenant: %s token: %s", url, tenant,
        request.headers().get(X_OKAPI_TOKEN)));

    return request.send();
  }

  public void put(String url, String tenant, Handler<HttpResponse<Buffer>> responseHandler,
      Handler<Throwable> exceptionHandler) {
    put(url, tenant, null, responseHandler, exceptionHandler);
  }

  public void put(String url, String tenant, MultiMap headers,
                  Handler<HttpResponse<Buffer>> responseHandler, Handler<Throwable> exceptionHandler) {
    put(url, tenant, headers)
        .onSuccess(res -> responseHandler.handle(res))
        .onFailure(cause -> exceptionHandler.handle(cause));
  }

  public Future<HttpResponse<Buffer>> put(String url, String tenant, MultiMap headers) {
    HttpRequest<Buffer> request = client.putAbs(url);

    if (headers != null) {
      request.headers().setAll(combineHeadersWithDefaults(headers));
    } else {
      request.headers().setAll(defaultHeaders);
    }

    logger.info(String.format("PUT %s tenant: %s token: %s", url, tenant,
        request.headers().get(X_OKAPI_TOKEN)));

    return request.send();
  }

  public void get(String url, String tenant, Handler<HttpResponse<Buffer>> responseHandler,
      Handler<Throwable> exceptionHandler) {
    get(url, tenant, null, responseHandler, exceptionHandler);
  }

  public void get(String url, String tenant, MultiMap headers,
                  Handler<HttpResponse<Buffer>> responseHandler, Handler<Throwable> exceptionHandler) {
    get(url, tenant, headers)
        .onSuccess(res -> responseHandler.handle(res))
        .onFailure(cause -> exceptionHandler.handle(cause));
  }

  public Future<HttpResponse<Buffer>> get(String url, String tenant, MultiMap headers) {
    HttpRequest<Buffer> request = client.getAbs(url);

    if (headers != null) {
      request.headers().setAll(combineHeadersWithDefaults(headers));
    } else {
      request.headers().setAll(defaultHeaders);
    }
    logger.info(String.format("GET %s tenant: %s token: %s", url, tenant,
        request.headers().get(X_OKAPI_TOKEN)));

    return request.send();
  }

  protected MultiMap combineHeadersWithDefaults(MultiMap headers) {
    MultiMap combined = null;

    if (headers != null) {
      headers.remove(HEADER_API_KEY);
      if (headers.size() > 0) {
        combined = MultiMap.caseInsensitiveMultiMap();
        combined.addAll(headers);
        for (Entry<String, String> entry : defaultHeaders.entries()) {
          if (!combined.contains(entry.getKey())) {
            combined.set(entry.getKey(), entry.getValue());
          }
        }
      }
    }
    return combined != null ? combined : defaultHeaders;
  }
}
