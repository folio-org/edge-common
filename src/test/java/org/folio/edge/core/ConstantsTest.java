package org.folio.edge.core;

import org.junit.Test;

public class ConstantsTest {
  @Test(expected = UnsupportedOperationException.class)
  public final void testUnmodifiableDepoymentOptions() {
    Constants.DEFAULT_DEPLOYMENT_OPTIONS.put("test", "invalid");
  }
}
