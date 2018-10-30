package org.folio.edge.core;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

public class ConstantsTest {
  @BeforeClass
  public static void setUpOnce() throws Exception {
    System.setProperty("port", "2112");
  }

  @Test(expected = UnsupportedOperationException.class)
  public final void testUnmodifiableDepoymentOptions() {
    Constants.DEFAULT_DEPLOYMENT_OPTIONS.put("test", "invalid");
  }

  @Test
  public final void testDepolymentOptionsWithEnvVars() {
    assertEquals(Integer.valueOf(2112), Constants.DEFAULT_DEPLOYMENT_OPTIONS.getInteger(Constants.SYS_PORT));
  }

  @Test
  public final void testDepolymentOptionsWithDefaults() {
    assertEquals(Constants.DEFAULT_LOG_LEVEL,
        Constants.DEFAULT_DEPLOYMENT_OPTIONS.getString(Constants.SYS_LOG_LEVEL));
    assertEquals(Constants.DEFAULT_API_KEY_SOURCES,
        Constants.DEFAULT_DEPLOYMENT_OPTIONS.getString(Constants.SYS_API_KEY_SOURCES));
    assertEquals(Long.valueOf(Constants.DEFAULT_REQUEST_TIMEOUT_MS),
        Constants.DEFAULT_DEPLOYMENT_OPTIONS.getLong(Constants.SYS_REQUEST_TIMEOUT_MS));
    assertEquals(Long.valueOf(Constants.DEFAULT_TOKEN_CACHE_TTL_MS),
        Constants.DEFAULT_DEPLOYMENT_OPTIONS.getLong(Constants.SYS_TOKEN_CACHE_TTL_MS));
    assertEquals(Long.valueOf(Constants.DEFAULT_NULL_TOKEN_CACHE_TTL_MS),
        Constants.DEFAULT_DEPLOYMENT_OPTIONS.getLong(Constants.SYS_NULL_TOKEN_CACHE_TTL_MS));
    assertEquals(Integer.valueOf(Constants.DEFAULT_TOKEN_CACHE_CAPACITY),
        Constants.DEFAULT_DEPLOYMENT_OPTIONS.getInteger(Constants.SYS_TOKEN_CACHE_CAPACITY));
    assertEquals(Constants.DEFAULT_SECURE_STORE_TYPE,
        Constants.DEFAULT_DEPLOYMENT_OPTIONS.getString(Constants.SYS_SECURE_STORE_TYPE));
  }
}
