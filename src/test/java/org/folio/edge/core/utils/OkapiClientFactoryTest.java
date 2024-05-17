package org.folio.edge.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.vertx.core.net.KeyStoreOptions;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;

import io.vertx.core.Vertx;

public class OkapiClientFactoryTest {

  private static final String OKAPI_URL = "http://mocked.okapi:9130";
  private static final Integer REQ_TIMEOUT_MS = 5000;
  private static final String KEYSTORE_TYPE = "some_keystore_type";
  private static final String KEYSTORE_PROVIDER = "some_keystore_provider";
  private static final String KEYSTORE_PATH = "some_keystore_path";
  private static final String KEYSTORE_PASSWORD = "some_keystore_password";
  private static final String KEY_ALIAS = "some_key_alias";
  private static final String KEY_ALIAS_PASSWORD = "some_key_alias_password";

  @Test
  public void testGetOkapiClientFactory() throws IllegalAccessException {
    Vertx vertx = Vertx.vertx();
    OkapiClientFactory ocf = new OkapiClientFactory(vertx, OKAPI_URL, REQ_TIMEOUT_MS);

    String okapiUrl = (String) FieldUtils.readDeclaredField(ocf, "okapiURL");
    Integer reqTimeoutMs = (Integer) FieldUtils.readDeclaredField(ocf, "reqTimeoutMs");
    KeyStoreOptions keyStoreOptions = (KeyStoreOptions) FieldUtils.readDeclaredField(ocf, "keyCertOptions", true);

    assertEquals(OKAPI_URL, okapiUrl);
    assertEquals(REQ_TIMEOUT_MS, reqTimeoutMs);
    assertNull(keyStoreOptions);
    OkapiClient client = ocf.getOkapiClient("tenant");
    assertNotNull(client);
  }

  @Test
  public void testGetSecuredOkapiClientFactory() throws IllegalAccessException {
    Vertx vertx = Vertx.vertx();
    OkapiClientFactory ocf = new OkapiClientFactory(vertx, OKAPI_URL, REQ_TIMEOUT_MS, KEYSTORE_TYPE, KEYSTORE_PROVIDER,
      KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_ALIAS_PASSWORD);

    String okapiUrl = (String) FieldUtils.readDeclaredField(ocf, "okapiURL");
    Integer reqTimeoutMs = (Integer) FieldUtils.readDeclaredField(ocf, "reqTimeoutMs");
    KeyStoreOptions keyStoreOptions = (KeyStoreOptions) FieldUtils.readDeclaredField(ocf, "keyCertOptions", true);

    assertEquals(OKAPI_URL, okapiUrl);
    assertEquals(REQ_TIMEOUT_MS, reqTimeoutMs);
    assertEquals(KEYSTORE_TYPE, keyStoreOptions.getType());
    assertEquals(KEYSTORE_PROVIDER, keyStoreOptions.getProvider());
    assertEquals(KEYSTORE_PATH, keyStoreOptions.getPath());
    assertEquals(KEYSTORE_PASSWORD, keyStoreOptions.getPassword());
    assertEquals(KEY_ALIAS, keyStoreOptions.getAlias());
    assertEquals(KEY_ALIAS_PASSWORD, keyStoreOptions.getAliasPassword());
    OkapiClient client = ocf.getOkapiClient("tenant");
    assertNotNull(client);
  }
}
