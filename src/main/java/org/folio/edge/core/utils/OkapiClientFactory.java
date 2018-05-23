package org.folio.edge.core.utils;

import io.vertx.core.Vertx;

public class OkapiClientFactory {

  public final String okapiURL;
  public final Vertx vertx;
  public final long reqTimeoutMs;

  public OkapiClientFactory(Vertx vertx, String okapiURL, long reqTimeoutMs) {
    this.vertx = vertx;
    this.okapiURL = okapiURL;
    this.reqTimeoutMs = reqTimeoutMs;
  }

  public OkapiClient getOkapiClient(String tenant) {
    return new OkapiClient(vertx, okapiURL, tenant, reqTimeoutMs);
  }
}
