package org.folio.edge.core;

import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.SYS_WEB_CLIENT_KEY_ALIAS;
import static org.folio.edge.core.Constants.SYS_WEB_CLIENT_KEY_ALIAS_PASSWORD;
import static org.folio.edge.core.Constants.SYS_WEB_CLIENT_SSL_ENABLED;
import static org.folio.edge.core.Constants.SYS_WEB_CLIENT_TRUSTSTORE_PASSWORD;
import static org.folio.edge.core.Constants.SYS_WEB_CLIENT_TRUSTSTORE_PATH;
import static org.folio.edge.core.Constants.SYS_WEB_CLIENT_TRUSTSTORE_PROVIDER;
import static org.folio.edge.core.Constants.SYS_WEB_CLIENT_TRUSTSTORE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.KeyStoreOptions;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.edge.core.utils.OkapiClientFactory;
import org.folio.edge.core.utils.OkapiClientFactoryInitializer;
import org.junit.Test;

public class OkapiClientInitializerTest {
  private static final String OKAPI_URL = "http://mocked.okapi:9130";
  private static final Integer REQ_TIMEOUT_MS = 5000;
  private static final String TRUSTSTORE_TYPE = "some_keystore_type";
  private static final String TRUSTSTORE_PROVIDER = "some_keystore_provider";
  private static final String TRUSTSTORE_PATH = "some_keystore_path";
  private static final String TRUSTSTORE_PASSWORD = "some_keystore_password";
  private static final String KEY_ALIAS = "some_key_alias";
  private static final String KEY_ALIAS_PASSWORD = "some_key_alias_password";

  @Test
  public void testGetOkapiClientFactory() throws IllegalAccessException {
    Vertx vertx = Vertx.vertx();
    JsonObject config = new JsonObject()
      .put(SYS_OKAPI_URL, OKAPI_URL)
      .put(SYS_REQUEST_TIMEOUT_MS, REQ_TIMEOUT_MS)
      .put(SYS_WEB_CLIENT_SSL_ENABLED, false);
    OkapiClientFactory ocf = OkapiClientFactoryInitializer.createInstance(vertx, config);

    String okapiUrl = (String) FieldUtils.readDeclaredField(ocf, "okapiURL");
    Integer reqTimeoutMs = (Integer) FieldUtils.readDeclaredField(ocf, "reqTimeoutMs");
    KeyStoreOptions keyStoreOptions = (KeyStoreOptions) FieldUtils.readDeclaredField(ocf, "trustOptions", true);

    assertEquals(OKAPI_URL, okapiUrl);
    assertEquals(REQ_TIMEOUT_MS, reqTimeoutMs);
    assertNull(keyStoreOptions);
    OkapiClient client = ocf.getOkapiClient("tenant");
    assertNotNull(client);
  }

  @Test
  public void testGetSecuredOkapiClientFactory() throws IllegalAccessException {
    Vertx vertx = Vertx.vertx();
    JsonObject config = new JsonObject()
      .put(SYS_OKAPI_URL, OKAPI_URL)
      .put(SYS_REQUEST_TIMEOUT_MS, REQ_TIMEOUT_MS)
      .put(SYS_WEB_CLIENT_SSL_ENABLED, true)
      .put(SYS_WEB_CLIENT_TRUSTSTORE_TYPE, TRUSTSTORE_TYPE)
      .put(SYS_WEB_CLIENT_TRUSTSTORE_PROVIDER, TRUSTSTORE_PROVIDER)
      .put(SYS_WEB_CLIENT_TRUSTSTORE_PATH, TRUSTSTORE_PATH)
      .put(SYS_WEB_CLIENT_TRUSTSTORE_PASSWORD, TRUSTSTORE_PASSWORD)
      .put(SYS_WEB_CLIENT_KEY_ALIAS, KEY_ALIAS)
      .put(SYS_WEB_CLIENT_KEY_ALIAS_PASSWORD, KEY_ALIAS_PASSWORD);
    OkapiClientFactory ocf = OkapiClientFactoryInitializer.createInstance(vertx, config);

    String okapiUrl = (String) FieldUtils.readDeclaredField(ocf, "okapiURL");
    Integer reqTimeoutMs = (Integer) FieldUtils.readDeclaredField(ocf, "reqTimeoutMs");
    KeyStoreOptions keyStoreOptions = (KeyStoreOptions) FieldUtils.readDeclaredField(ocf, "trustOptions", true);

    assertEquals(OKAPI_URL, okapiUrl);
    assertEquals(REQ_TIMEOUT_MS, reqTimeoutMs);
    assertEquals(TRUSTSTORE_TYPE, keyStoreOptions.getType());
    assertEquals(TRUSTSTORE_PROVIDER, keyStoreOptions.getProvider());
    assertEquals(TRUSTSTORE_PATH, keyStoreOptions.getPath());
    assertEquals(TRUSTSTORE_PASSWORD, keyStoreOptions.getPassword());
    assertEquals(KEY_ALIAS, keyStoreOptions.getAlias());
    assertEquals(KEY_ALIAS_PASSWORD, keyStoreOptions.getAliasPassword());
    OkapiClient client = ocf.getOkapiClient("tenant");
    assertNotNull(client);
  }
}
