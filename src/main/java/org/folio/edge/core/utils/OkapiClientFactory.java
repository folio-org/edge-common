package org.folio.edge.core.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.Vertx;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.KeyStoreOptions;

public class OkapiClientFactory {

  private final Map<String, OkapiClient> cache = new ConcurrentHashMap<>();

  public final String okapiURL;
  public final Vertx vertx;
  public final int reqTimeoutMs;
  private KeyCertOptions keyCertOptions;

  public OkapiClientFactory(Vertx vertx, String okapiURL, int reqTimeoutMs) {
    this.vertx = vertx;
    this.okapiURL = okapiURL;
    this.reqTimeoutMs = reqTimeoutMs;
  }

  public OkapiClientFactory(Vertx vertx,
                            String okapiURL,
                            int reqTimeoutMs,
                            String keystoreType,
                            String keystoreProvider,
                            String keystorePath,
                            String keystorePassword,
                            String keyAlias,
                            String keyAliasPassword) {
    this(vertx, okapiURL, reqTimeoutMs);
    this.keyCertOptions = new KeyStoreOptions()
      .setType(keystoreType)
      .setProvider(keystoreProvider)
      .setPath(keystorePath)
      .setPassword(keystorePassword)
      .setAlias(keyAlias)
      .setAliasPassword(keyAliasPassword);
  }

  public OkapiClient getOkapiClient(String tenant) {
    if (keyCertOptions == null) {
      return cache.computeIfAbsent(tenant, t -> new OkapiClient(vertx, okapiURL, t, reqTimeoutMs));
    } else {
      return cache.computeIfAbsent(tenant, t -> new OkapiClient(vertx, okapiURL, t, reqTimeoutMs, keyCertOptions));
    }
  }
}
