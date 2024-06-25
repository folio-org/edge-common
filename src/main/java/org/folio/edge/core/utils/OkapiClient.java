package org.folio.edge.core.utils;

import com.amazonaws.util.StringUtils;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.net.TrustOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.cache.TokenCacheFactory;
import org.folio.okapi.common.WebClientFactory;
import org.folio.okapi.common.refreshtoken.client.Client;
import org.folio.okapi.common.refreshtoken.client.ClientOptions;

import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.HEADER_API_KEY;
import static org.folio.edge.core.Constants.JSON_OR_TEXT;
import static org.folio.edge.core.Constants.X_OKAPI_TENANT;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;

public class OkapiClient {

  private static final Logger logger = LogManager.getLogger(OkapiClient.class);

  public final String okapiURL;
  public final WebClient client;
  public final String tenant;
  public String secondaryTenantId;
  public final int reqTimeout;
  public final Vertx vertx;
  Client tokenClient;
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

  public OkapiClient(OkapiClient client, String secondaryTenantId) {
    this(client);
    this.secondaryTenantId = secondaryTenantId;
    if (!StringUtils.isNullOrEmpty(secondaryTenantId)) {
      defaultHeaders.set(X_OKAPI_TENANT, secondaryTenantId);
    }
  }

  protected OkapiClient(Vertx vertx, String okapiURL, String tenant, int timeout) {
    this.vertx = vertx;
    this.reqTimeout = timeout;
    this.okapiURL = okapiURL;
    this.tenant = tenant;
    WebClientOptions options = initDefaultWebClientOptions(timeout);
    client = WebClientFactory.getWebClient(vertx, options);
    initDefaultHeaders();
  }

  /**
   * Create Okapi client configured to work in SSL/TLS mode.
   * Trust options can be null.
   */
  protected OkapiClient(Vertx vertx, String okapiURL, String tenant, int timeout, TrustOptions trustOptions) {
    this.vertx = vertx;
    this.reqTimeout = timeout;
    this.okapiURL = okapiURL;
    this.tenant = tenant;
    WebClientOptions options = initDefaultWebClientOptions(timeout)
      .setSsl(true);
    if (trustOptions != null) {
      options.setTrustOptions(trustOptions);
      options.setVerifyHost(false); //Hardcoded now. Later it could be configurable using env vars.
    }
    client = WebClientFactory.getWebClient(vertx, options);
    initDefaultHeaders();
  }

  protected OkapiClient(Vertx vertx, String okapiURL, String tenant, String secondaryTenantId, int timeout) {
    this(vertx, okapiURL, tenant, timeout);
    this.secondaryTenantId = secondaryTenantId;
    if (!StringUtils.isNullOrEmpty(secondaryTenantId)) {
      defaultHeaders.set(X_OKAPI_TENANT, secondaryTenantId);
    }
  }

  protected void initDefaultHeaders() {
    defaultHeaders.add(HttpHeaders.ACCEPT_ENCODING, HttpHeaders.DEFLATE_GZIP);
    defaultHeaders.add(HttpHeaders.ACCEPT.toString(), JSON_OR_TEXT);
    defaultHeaders.add(HttpHeaders.CONTENT_TYPE.toString(), APPLICATION_JSON);
    defaultHeaders.add(X_OKAPI_TENANT, tenant);
  }

  protected WebClientOptions initDefaultWebClientOptions(int timeout) {
    return new WebClientOptions().setTryUseCompression(true)
      .setIdleTimeoutUnit(TimeUnit.MILLISECONDS).setIdleTimeout(timeout)
      .setConnectTimeout(timeout);
  }

  public CompletableFuture<String> login(String username, String password) {
    return doLogin(username, password, null).toCompletionStage().toCompletableFuture();
  }

  public Future<String> doLogin(String username, String password) {
    return doLogin(username, password, null);
  }

  /**
   * Login.
   *
   * @param headers additional HTTP headers to combine with {@link #defaultHeaders}, can be null or empty.
   *     NEVER pass request.headers(), this is vulnerable to header injection!
   *     NEVER use a deny list like request.headers().remove(HttpHeaders.ACCEPT),
   *     this is vulnerable to header injection! If a header from the request is needed use a white list:
   *     {@code new MultiMap.caseInsensitiveMultiMap().add("X-Myheader", request.headers().get("X-Myheader"))}
   */
  public CompletableFuture<String> login(String username, String password, MultiMap headers) {
    return doLogin(username, password, headers).toCompletionStage().toCompletableFuture();
  }

  /**
   * Login.
   *
   * @param headers additional HTTP headers to combine with {@link #defaultHeaders}, can be null or empty.
   *     NEVER pass request.headers(), this is vulnerable to header injection!
   *     NEVER use a deny list like request.headers().remove(HttpHeaders.ACCEPT),
   *     this is vulnerable to header injection! If a header from the request is needed use a white list:
   *     {@code new MultiMap.caseInsensitiveMultiMap().add("X-Myheader", request.headers().get("X-Myheader"))}
   */
  public Future<String> doLogin(String username, String password, MultiMap headers) {
    return loginWithSupplier(username, () -> Future.succeededFuture(password));
  }

  public Future<String> loginWithSupplier(String username, Supplier<Future<String>> getPasswordSupplier) {
    logger.info("loginWithSupplier username={} cache={}", username, TokenCacheFactory.get());
    ClientOptions clientOptions = new ClientOptions()
        .okapiUrl(okapiURL)
        .webClient(client);
    tokenClient = Client.createLoginClient(clientOptions, TokenCacheFactory.get(),
            tenant, username, getPasswordSupplier);
    return tokenClient.getToken().map(token -> {
      setToken(token);
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
    return defaultHeaders.get(X_OKAPI_TOKEN);
  }

  public void setToken(String token) {
    if (token == null) {
      defaultHeaders.remove(X_OKAPI_TOKEN);
    } else {
      defaultHeaders.set(X_OKAPI_TOKEN, token);
    }
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
      setToken(token);
      return request;
    });
  }

  /**
   * Send POST request.
   *
   * @param headers additional HTTP headers to combine with {@link #defaultHeaders}, can be null or empty.
   *     NEVER pass request.headers(), this is vulnerable to header injection!
   *     NEVER use a deny list like request.headers().remove(HttpHeaders.ACCEPT),
   *     this is vulnerable to header injection! If a header from the request is needed use a white list:
   *     {@code new MultiMap.caseInsensitiveMultiMap().add("X-Myheader", request.headers().get("X-Myheader"))}
   */
  public void post(String url, String tenant, String payload, MultiMap headers,
                   Handler<HttpResponse<Buffer>> responseHandler,
                   Handler<Throwable> exceptionHandler) {
    post(url, tenant, payload, headers)
        .onSuccess(responseHandler)
        .onFailure(exceptionHandler);
  }

  /**
   * Send POST request.
   *
   * @param headers additional HTTP headers to combine with {@link #defaultHeaders}, can be null or empty.
   *     NEVER pass request.headers(), this is vulnerable to header injection!
   *     NEVER use a deny list like request.headers().remove(HttpHeaders.ACCEPT),
   *     this is vulnerable to header injection! If a header from the request is needed use a white list:
   *     {@code new MultiMap.caseInsensitiveMultiMap().add("X-Myheader", request.headers().get("X-Myheader"))}
   */
  public Future<HttpResponse<Buffer>> post(String url, String tenant, String payload, MultiMap headers) {
    return prepareTokenAndHeaders(client.postAbs(url), headers).compose(request -> {
      logger.info("POST {} tenant: {}", url, tenant);
      if (payload != null) {
        logger.trace("Payload {}", payload);
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

  /**
   * Send DELETE request.
   *
   * @param headers additional HTTP headers to combine with {@link #defaultHeaders}, can be null or empty.
   *     NEVER pass request.headers(), this is vulnerable to header injection!
   *     NEVER use a deny list like request.headers().remove(HttpHeaders.ACCEPT),
   *     this is vulnerable to header injection! If a header from the request is needed use a white list:
   *     {@code new MultiMap.caseInsensitiveMultiMap().add("X-Myheader", request.headers().get("X-Myheader"))}
   */
  public void delete(String url, String tenant, MultiMap headers,
                     Handler<HttpResponse<Buffer>> responseHandler,
                     Handler<Throwable> exceptionHandler) {
    delete(url, tenant, headers)
        .onSuccess(responseHandler)
        .onFailure(exceptionHandler);
  }

  /**
   * Send DELETE request.
   *
   * @param headers additional HTTP headers to combine with {@link #defaultHeaders}, can be null or empty.
   *     NEVER pass request.headers(), this is vulnerable to header injection!
   *     NEVER use a deny list like request.headers().remove(HttpHeaders.ACCEPT),
   *     this is vulnerable to header injection! If a header from the request is needed use a white list:
   *     {@code new MultiMap.caseInsensitiveMultiMap().add("X-Myheader", request.headers().get("X-Myheader"))}
   */
  public Future<HttpResponse<Buffer>> delete(String url, String tenant, MultiMap headers) {
    return prepareTokenAndHeaders(client.deleteAbs(url), headers).compose(request -> {
      logger.info("DELETE {} tenant: {}", url, tenant);
      return request.send();
    });
  }

  public void put(String url, String tenant, Handler<HttpResponse<Buffer>> responseHandler,
      Handler<Throwable> exceptionHandler) {
    put(url, tenant, null, responseHandler, exceptionHandler);
  }

  /**
   * Send PUT request.
   *
   * @param headers additional HTTP headers to combine with {@link #defaultHeaders}, can be null or empty.
   *     NEVER pass request.headers(), this is vulnerable to header injection!
   *     NEVER use a deny list like request.headers().remove(HttpHeaders.ACCEPT),
   *     this is vulnerable to header injection! If a header from the request is needed use a white list:
   *     {@code new MultiMap.caseInsensitiveMultiMap().add("X-Myheader", request.headers().get("X-Myheader"))}
   */
  public void put(String url, String tenant, MultiMap headers,
                  Handler<HttpResponse<Buffer>> responseHandler, Handler<Throwable> exceptionHandler) {
    put(url, tenant, headers)
        .onSuccess(responseHandler)
        .onFailure(exceptionHandler);
  }

  /**
   * Send PUT request.
   *
   * @param headers additional HTTP headers to combine with {@link #defaultHeaders}, can be null or empty.
   *     NEVER pass request.headers(), this is vulnerable to header injection!
   *     NEVER use a deny list like request.headers().remove(HttpHeaders.ACCEPT),
   *     this is vulnerable to header injection! If a header from the request is needed use a white list:
   *     {@code new MultiMap.caseInsensitiveMultiMap().add("X-Myheader", request.headers().get("X-Myheader"))}
   */
  public Future<HttpResponse<Buffer>> put(String url, String tenant, MultiMap headers) {
    return prepareTokenAndHeaders(client.putAbs(url), headers).compose(request -> {
      logger.info("PUT {} tenant: {}", url, tenant);
      return request.send();
    });
  }

  public void get(String url, String tenant, Handler<HttpResponse<Buffer>> responseHandler,
      Handler<Throwable> exceptionHandler) {
    get(url, tenant, null, responseHandler, exceptionHandler);
  }

  /**
   * Send GET request.
   *
   * @param headers additional HTTP headers to combine with {@link #defaultHeaders}, can be null or empty.
   *     NEVER pass request.headers(), this is vulnerable to header injection!
   *     NEVER use a deny list like request.headers().remove(HttpHeaders.ACCEPT),
   *     this is vulnerable to header injection! If a header from the request is needed use a white list:
   *     {@code new MultiMap.caseInsensitiveMultiMap().add("X-Myheader", request.headers().get("X-Myheader"))}
   */
  public void get(String url, String tenant, MultiMap headers,
                  Handler<HttpResponse<Buffer>> responseHandler, Handler<Throwable> exceptionHandler) {
    get(url, tenant, headers)
        .onSuccess(responseHandler)
        .onFailure(exceptionHandler);
  }

  /**
   * Send GET request.
   *
   * @param headers additional HTTP headers to combine with {@link #defaultHeaders}, can be null or empty.
   *     NEVER pass request.headers(), this is vulnerable to header injection!
   *     NEVER use a deny list like request.headers().remove(HttpHeaders.ACCEPT),
   *     this is vulnerable to header injection! If a header from the request is needed use a white list:
   *     {@code new MultiMap.caseInsensitiveMultiMap().add("X-Myheader", request.headers().get("X-Myheader"))}
   */
  public Future<HttpResponse<Buffer>> get(String url, String tenant, MultiMap headers) {
    return prepareTokenAndHeaders(client.getAbs(url), headers).compose(request -> {
      logger.info("GET {} tenant: {}", url, tenant);
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
    // don't allow to overwrite tenant or token.
    // Tenant can be configured in the settings only,
    // token can be set using login/doLogin/setToken only.
    // https://issues.folio.org/browse/EDGCOMMON-47
    // https://issues.folio.org/browse/EDGCOMMON-59
    combined.remove(X_OKAPI_TENANT);
    combined.remove(X_OKAPI_TOKEN);
    for (Entry<String, String> entry : defaultHeaders.entries()) {
      if (!combined.contains(entry.getKey())) {
        combined.set(entry.getKey(), entry.getValue());
      }
    }
    return combined;
  }
}
