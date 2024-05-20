package org.folio.edge.core.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.Vertx;
import io.vertx.core.net.TrustOptions;

public class OkapiClientFactory {

  private final Map<String, OkapiClient> cache = new ConcurrentHashMap<>();

  public final String okapiURL;
  public final Vertx vertx;
  public final int reqTimeoutMs;
  private boolean sslMode;
  private TrustOptions trustOptions;

  public OkapiClientFactory(Vertx vertx, String okapiURL, int reqTimeoutMs) {
    this.vertx = vertx;
    this.okapiURL = okapiURL;
    this.reqTimeoutMs = reqTimeoutMs;
  }

  public OkapiClientFactory(Vertx vertx, String okapiURL, int reqTimeoutMs, TrustOptions trustOptions) {
    this(vertx, okapiURL, reqTimeoutMs);
    this.sslMode = true;
    this.trustOptions = trustOptions;
  }

  public OkapiClient getOkapiClient(String tenant) {
    if (sslMode) {
      return cache.computeIfAbsent(tenant, t -> new OkapiClient(vertx, okapiURL, t, reqTimeoutMs, trustOptions));
    } else {
      return cache.computeIfAbsent(tenant, t -> new OkapiClient(vertx, okapiURL, t, reqTimeoutMs));
    }
  }
}
