package org.folio.edge.core.utils;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.core.Constants.X_OKAPI_TENANT;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;

import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import org.apache.log4j.Logger;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;

public class OkapiClient {

  private static final Logger logger = Logger.getLogger(OkapiClient.class);

  public final String okapiURL;
  public final HttpClient client;
  public final String tenant;
  public final long reqTimeout;
    
  protected final MultiMap defaultHeaders = MultiMap.caseInsensitiveMultiMap();
  
  protected OkapiClient(Vertx vertx, String okapiURL, String tenant, long timeout) {
    this.reqTimeout = timeout;
    this.okapiURL = okapiURL;
    this.tenant = tenant;
    this.client = vertx.createHttpClient(new HttpClientOptions().setKeepAlive(false));
  }

  public CompletableFuture<String> login(String username, String password) {
    return login(username, password, null);
  }

  public CompletableFuture<String> login(String username, String password, MultiMap headers) {
    CompletableFuture<String> future = new CompletableFuture<>();

    MultiMap combined = null;
    if (headers != null && headers.size() > 0) {
      combined = MultiMap.caseInsensitiveMultiMap();
      combined.addAll(headers);
      for (Entry<String, String> entry : defaultHeaders.entries()) {
        combined.set(entry.getKey(), entry.getValue());
      }
    }

    JsonObject payload = new JsonObject();
    payload.put("username", username);
    payload.put("password", password == null ? "" : password);

    post(
        okapiURL + "/authn/login",
        tenant,
        payload.encode(),
        combined != null ? combined : defaultHeaders,
        response -> response.bodyHandler(body -> {
          if (response.statusCode() == 201) {
            logger.info("Successfully logged into FOLIO");
            String token = response.getHeader(X_OKAPI_TOKEN);
            setToken(token);
            future.complete(token);
          } else {
            logger.warn(String.format(
                "Failed to log into FOLIO: (%s) %s",
                response.statusCode(),
                body.toString()));
            future.complete(null);
          }
        }),
        t -> {
          logger.error("Exception: " + t.getMessage());
          future.completeExceptionally(t);
        });
    return future;
  }

  public CompletableFuture<Boolean> healthy() {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    get(
        okapiURL + "/_/proxy/health",
        tenant, response -> response.bodyHandler(body -> {
          int status = response.statusCode();
          if (status == 200) {
            future.complete(true);
          } else {
            logger.error(String.format("OKAPI is unhealthy! status: %s body: %s", status, body.toString()));
            future.complete(false);
          }
        }),
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

  public void post(String url, String tenant, String payload, Handler<HttpClientResponse> responseHandler,
      Handler<Throwable> exceptionHandler) {
    post(url, tenant, payload, null, responseHandler, exceptionHandler);
  }

  public void post(String url, String tenant, String payload, MultiMap headers,
      Handler<HttpClientResponse> responseHandler, Handler<Throwable> exceptionHandler) {

    if (logger.isTraceEnabled())
      logger.trace("POST " + url + " Request: " + payload);

    final HttpClientRequest request = client.postAbs(url);

    // safe to assume application/json. I *think* Caller can still
    // override
    request.putHeader(HttpHeaders.CONTENT_TYPE.toString(), APPLICATION_JSON)
      .putHeader(HttpHeaders.ACCEPT.toString(), String.format("%s, %s", APPLICATION_JSON, TEXT_PLAIN))
      .putHeader(X_OKAPI_TENANT, tenant);

    if (headers != null) {
      request.headers().addAll(headers);
    }

    request.handler(responseHandler)
      .exceptionHandler(exceptionHandler)
      .setTimeout(reqTimeout)
      .end(payload);
  }

  public void get(String url, String tenant, Handler<HttpClientResponse> responseHandler,
      Handler<Throwable> exceptionHandler) {
    get(url, tenant, null, responseHandler, exceptionHandler);
  }

  public void get(String url, String tenant, MultiMap headers,
      Handler<HttpClientResponse> responseHandler, Handler<Throwable> exceptionHandler) {

    final HttpClientRequest request = client.getAbs(url);

    if (headers != null) {
      request.headers().addAll(headers);
    }

    logger.info(String.format("GET %s tenant: %s token: %s", url, tenant,
        request.headers().get(X_OKAPI_TOKEN)));

    request.handler(responseHandler)
      .exceptionHandler(exceptionHandler)
      .setTimeout(reqTimeout)
      .end();
  }

  public void close() {
    client.close();
  }
}
