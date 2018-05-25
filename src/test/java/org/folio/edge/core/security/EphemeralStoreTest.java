package org.folio.edge.core.security;

import static org.folio.edge.core.utils.test.TestUtils.assertLogMessage;
import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

public class EphemeralStoreTest {

  private Properties props;
  private EphemeralStore store;

  @Before
  public void setUp() throws Exception {
    props = new Properties();
    props.setProperty(EphemeralStore.PROP_TENANTS, "dit ,dat, dot,done");
    props.setProperty("dit", "dit_password");
    props.setProperty("dat", "dat_password");
    props.setProperty("dot", "dot_password");

    store = new EphemeralStore(props);
  }

  @Test
  public void testConstructor() {
    assertLogMessage(EphemeralStore.logger, 1, 2, Level.WARN, "Attention: No credentials were found/loaded", null,
        () -> {
          new EphemeralStore(null);
        });

    assertLogMessage(EphemeralStore.logger, 1, 2, Level.WARN, "Attention: No credentials were found/loaded", null,
        () -> {
          new EphemeralStore(new Properties());
        });
  }

  @Test
  public void testGet() {
    assertEquals(4, store.store.size());
    assertEquals("dit_password", store.get("dit", "dit"));
    assertEquals("dot_password", store.get("dot", "dot"));
    assertEquals("dat_password", store.get("dat", "dat"));
    assertEquals("", store.get("done", "done"));
    assertEquals(null, store.get(null, null));
  }

  @Test
  public void testGetKey() {
    assertEquals("tenant_username", store.getKey("tenant", "username"));
    assertEquals("tenant_null", store.getKey("tenant", null));
    assertEquals("null_username", store.getKey(null, "username"));
    assertEquals("null_null", store.getKey(null, null));
  }

}
