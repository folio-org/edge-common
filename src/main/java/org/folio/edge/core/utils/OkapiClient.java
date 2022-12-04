package org.folio.edge.core.utils;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.HEADER_API_KEY;
import static org.folio.edge.core.Constants.JSON_OR_TEXT;
import static org.folio.edge.core.Constants.X_OKAPI_TENANT;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;

import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.cache.TokenCacheFactory;
import org.folio.okapi.common.WebClientFactory;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.vertx.login.TokenClient;
import org.folio.vertx.tokencache.TokenCache;

public class OkapiClient {

  private static final Logger logger = LogManager.getLogger(OkapiClient.class);

  public final String okapiURL;
  public final WebClient client;
  public final String tenant;
  public final int reqTimeout;
  public final Vertx vertx;
  TokenClient tokenClient;
  String token;

  protected final MultiMap defaultHeaders = MultiMap.caseInsensitiveMultiMap();

  public OkapiClient(OkapiClient client) {
    this.vertx = client.vertx;
    this.reqTimeout = client.reqTimeout;
    this.tenant = client.tenant;
    this.okapiURL = client.okapiURL;
    this.client = client.client;
    this.setToken(client.getToken());
    this.tokenClient = client.tokenClient;
    initDefaultHeaders();
  }

  protected OkapiClient(Vertx vertx, String okapiURL, String tenant, int timeout) {
    this.vertx = vertx;
    this.reqTimeout = timeout;
    this.okapiURL = okapiURL;
    this.tenant = tenant;
    WebClientOptions options = new WebClientOptions().setTryUseCompression(true)
        .setIdleTimeoutUnit(TimeUnit.MILLISECONDS).setIdleTimeout(timeout)
        .setConnectTimeout(timeout);
    client = WebClientFactory.getWebClient(vertx, options);
    initDefaultHeaders();
  }

  protected void initDefaultHeaders() {
    defaultHeaders.add(HttpHeaders.ACCEPT_ENCODING, HttpHeaders.DEFLATE_GZIP);
    defaultHeaders.add(HttpHeaders.ACCEPT.toString(), JSON_OR_TEXT);
    defaultHeaders.add(HttpHeaders.CONTENT_TYPE.toString(), APPLICATION_JSON);
    defaultHeaders.add(X_OKAPI_TENANT, tenant);
  }

  @Deprecated
  public CompletableFuture<String> login(String username, String password) {
    return doLogin(username, password, null).toCompletionStage().toCompletableFuture();
  }

  @Deprecated
  public Future<String> doLogin(String username, String password) {
    return doLogin(username, password, null);
  }

  @Deprecated
  public CompletableFuture<String> login(String username, String password, MultiMap headers) {
    return doLogin(username, password, headers).toCompletionStage().toCompletableFuture();
  }

  @Deprecated
  public Future<String> doLogin(String username, String password, MultiMap headers) {
    return loginWithSupplier(username, () -> Future.succeededFuture(password));
  }

  public Future<String> loginWithSupplier(String username, Supplier<Future<String>> getPasswordSupplier) {
    logger.info("loginWithSupplier username={} cache={}", username, TokenCacheFactory.get());
    tokenClient = new TokenClient(okapiURL, client, TokenCacheFactory.get(),
            tenant, username, getPasswordSupplier);
    return tokenClient.getToken().map(token -> {
      this.token = token;
      return token;
    });
  }

  public CompletableFuture<Boolean> healthy() {
    return health().toCompletionStage().toCompletableFuture();
  }

  public Future<Boolean> health() {
    return get(okapiURL + "/_/proxy/health", tenant, null)
        .map(response -> {
          int status = response.statusCode();
          if (status == 200) {
            return true;
          }
          logger.error("OKAPI is unhealthy! status: {} body: {}",
              () -> status, response::bodyAsString);
          return false;
        })
        .otherwise(t -> {
          logger.error("Exception checking OKAPI's health: {}", t.getMessage(), t);
          return false;
        });
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public void post(String url, String tenant, String payload, Handler<HttpResponse<Buffer>> responseHandler,
      Handler<Throwable> exceptionHandler) {
    post(url, tenant, payload, null, responseHandler, exceptionHandler);
  }

  Future<HttpRequest<Buffer>> prepareTokenAndHeaders(HttpRequest<Buffer> request, MultiMap headers) {
    if (headers != null) {
      request.headers().setAll(combineHeadersWithDefaults(headers));
    } else {
      request.headers().setAll(defaultHeaders);
    }

    request.timeout(reqTimeout);

    if (tokenClient == null) {
      return Future.succeededFuture(request);
    }
    return tokenClient.getToken().map(token -> {
      request.putHeader(X_OKAPI_TOKEN, token);
      this.token = token;
      return request;
    });
  }

  public void post(String url, String tenant, String payload, MultiMap headers,
                   Handler<HttpResponse<Buffer>> responseHandler,
                   Handler<Throwable> exceptionHandler) {
    post(url, tenant, payload, headers)
        .onSuccess(responseHandler)
        .onFailure(exceptionHandler);
  }

  public Future<HttpResponse<Buffer>> post(String url, String tenant, String payload, MultiMap headers) {
    return prepareTokenAndHeaders(client.postAbs(url), headers).compose(request -> {
      logger.info("POST {} tenant: {} token: {}", () -> url, () -> tenant, () -> token);
      if (payload != null) {
        logger.trace("Payload {}", () -> payload);
        return request.sendBuffer(Buffer.buffer(payload));
      } else {
        return request.send();
      }
    });
  }

  public void delete(String url, String tenant, Handler<HttpResponse<Buffer>> responseHandler,
      Handler<Throwable> exceptionHandler) {
    delete(url, tenant, null, responseHandler, exceptionHandler);
  }

  public void delete(String url, String tenant, MultiMap headers,
                     Handler<HttpResponse<Buffer>> responseHandler,
                     Handler<Throwable> exceptionHandler) {
    delete(url, tenant, headers)
        .onSuccess(responseHandler)
        .onFailure(exceptionHandler);
  }

  public Future<HttpResponse<Buffer>> delete(String url, String tenant, MultiMap headers) {
    return prepareTokenAndHeaders(client.deleteAbs(url), headers).compose(request -> {
      logger.info("DELETE {} tenant: {} token: {}", () -> url, () -> tenant, () -> token);
      return request.send();
    });
  }

  public void put(String url, String tenant, Handler<HttpResponse<Buffer>> responseHandler,
      Handler<Throwable> exceptionHandler) {
    put(url, tenant, null, responseHandler, exceptionHandler);
  }

  public void put(String url, String tenant, MultiMap headers,
                  Handler<HttpResponse<Buffer>> responseHandler, Handler<Throwable> exceptionHandler) {
    put(url, tenant, headers)
        .onSuccess(responseHandler)
        .onFailure(exceptionHandler);
  }

  public Future<HttpResponse<Buffer>> put(String url, String tenant, MultiMap headers) {
    return prepareTokenAndHeaders(client.putAbs(url), headers).compose(request -> {
      logger.info("PUT {} tenant: {} token: {}", () -> url, () -> tenant, () -> token);
      return request.send();
    });
  }

  public void get(String url, String tenant, Handler<HttpResponse<Buffer>> responseHandler,
      Handler<Throwable> exceptionHandler) {
    get(url, tenant, null, responseHandler, exceptionHandler);
  }

  public void get(String url, String tenant, MultiMap headers,
                  Handler<HttpResponse<Buffer>> responseHandler, Handler<Throwable> exceptionHandler) {
    get(url, tenant, headers)
        .onSuccess(responseHandler)
        .onFailure(exceptionHandler);
  }

  public Future<HttpResponse<Buffer>> get(String url, String tenant, MultiMap headers) {
    return prepareTokenAndHeaders(client.getAbs(url), headers).compose(request -> {
      logger.info("GET {} tenant: {} token: {}", () -> url, () -> tenant, () -> token);
      return request.send();
    });
  }

  protected MultiMap combineHeadersWithDefaults(MultiMap headers) {
    if (headers == null || headers.isEmpty()) {
      return defaultHeaders;
    }

    MultiMap combined = MultiMap.caseInsensitiveMultiMap();
    combined.addAll(headers);
    // remove from combined because headers might not be case insensitive
    combined.remove(HEADER_API_KEY);
    combined.remove(X_OKAPI_TENANT);  // don't allow to overwrite: https://issues.folio.org/browse/EDGCOMMON-47
    for (Entry<String, String> entry : defaultHeaders.entries()) {
      if (!combined.contains(entry.getKey())) {
        combined.set(entry.getKey(), entry.getValue());
      }
    }
    return combined;
  }
}
