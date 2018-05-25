package org.folio.edge.core.security;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.api.Logical;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.rest.RestResponse;

public class VaultStoreTest {

  @Mock
  Vault vault;

  @InjectMocks
  VaultStore secureStore;

  @Before
  public void setUp() throws Exception {

    Properties props = new Properties();

    secureStore = new VaultStore(props);

    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGet() throws Exception {
    String password = "Pa$$w0rd";

    LogicalResponse successResp = new LogicalResponse(
        new RestResponse(
            200,
            APPLICATION_JSON,
            ("{\"data\":{\"diku\":\"" + password + "\"}}").getBytes()),
        0);

    LogicalResponse failureResp = new LogicalResponse(
        new RestResponse(
            404,
            APPLICATION_JSON,
            "{\"errors\":[]}".getBytes()),
        0);

    Logical logical = mock(Logical.class);
    when(vault.logical()).thenReturn(logical);
    when(logical.read("secret/diku")).thenReturn(successResp);
    when(logical.read("secret/bogus")).thenReturn(failureResp);

    assertEquals(password, secureStore.get("diku", "diku"));
    assertNull(secureStore.get("bogus", "bogus"));
  }

  // TODO Add test coverage for SSL/TLS configuration
}
