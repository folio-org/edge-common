package org.folio.edge.core.model;

public class ClientInfo {

  public final String tenantId;
  public final String username;
  public final String clientId;

  public ClientInfo(String clientId, String tenantId, String username) {
    this.tenantId = tenantId;
    this.username = username;
    this.clientId = clientId;
  }

}
