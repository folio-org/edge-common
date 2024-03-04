package org.folio.edge.core.security;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.folio.edge.core.security.SecureStore.NotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.api.Logical;
import io.github.jopenlibs.vault.response.LogicalResponse;
import io.github.jopenlibs.vault.rest.RestResponse;

public class VaultStoreTest {

  @Mock
  Vault vault;

  @InjectMocks
  VaultStore secureStore;

  private AutoCloseable mocks;

  @Before
  public void setUp() throws Exception {

    Properties props = new Properties();

    secureStore = new VaultStore(props);

    mocks = MockitoAnnotations.openMocks(this);
  }

  @After
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test(expected = NotFoundException.class)
  public void testGet() throws Exception {
    String password = "Pa$$w0rd";
    String tenant = "diku";
    String clientId = "abcdef1234";

    LogicalResponse successResp = new LogicalResponse(
        new RestResponse(
            200,
            APPLICATION_JSON,
            String.format("{\"data\":{\"%s\":\"%s\"}}", tenant, password).getBytes()),
        0,
        Logical.logicalOperations.readV2);

    LogicalResponse failureResp = new LogicalResponse(
        new RestResponse(
            404,
            APPLICATION_JSON,
            "{\"errors\":[]}".getBytes()),
        0,
        Logical.logicalOperations.readV2);

    Logical logical = mock(Logical.class);
    when(vault.logical()).thenReturn(logical);
    when(logical.read(clientId + "/" + tenant)).thenReturn(successResp);
    when(logical.read(clientId + "/bogus")).thenReturn(failureResp);

    assertEquals(password, secureStore.get(clientId, "diku", "diku"));
    secureStore.get(clientId, "bogus", "bogus");
  }

  // TODO Add test coverage for SSL/TLS configuration
}
