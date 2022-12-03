package org.folio.edge.core.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.Vertx;
import org.folio.edge.core.model.ClientInfo;
import org.folio.vertx.login.TokenClient;

public class OkapiClientFactory {

  private final Map<String, OkapiClient> cache = new ConcurrentHashMap<>();

  public final String okapiURL;
  public final Vertx vertx;
  public final int reqTimeoutMs;

  public OkapiClientFactory(Vertx vertx, String okapiURL, int reqTimeoutMs) {
    this.vertx = vertx;
    this.okapiURL = okapiURL;
    this.reqTimeoutMs = reqTimeoutMs;
  }

  public OkapiClient getOkapiClient(String tenant) {
    return cache.computeIfAbsent(tenant, t -> new OkapiClient(vertx, okapiURL, t, reqTimeoutMs));
  }
}
