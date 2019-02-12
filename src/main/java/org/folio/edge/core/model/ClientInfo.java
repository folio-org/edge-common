package org.folio.edge.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClientInfo {

  @JsonProperty("t")
  public final String tenantId;

  @JsonProperty("u")
  public final String username;

  @JsonProperty("s")
  public final String salt;

  @JsonCreator
  public ClientInfo(@JsonProperty("s") String salt, @JsonProperty("t") String tenantId,
      @JsonProperty("u") String username) {
    this.tenantId = tenantId;
    this.username = username;
    this.salt = salt;
  }

}
