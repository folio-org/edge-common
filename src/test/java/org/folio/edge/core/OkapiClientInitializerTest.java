package org.folio.edge.core;

import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.FOLIO_CLIENT_TLS_ENABLED;
import static org.folio.edge.core.Constants.FOLIO_CLIENT_TLS_TRUSTSTOREPASSWORD;
import static org.folio.edge.core.Constants.FOLIO_CLIENT_TLS_TRUSTSTOREPATH;
import static org.folio.edge.core.Constants.FOLIO_CLIENT_TLS_TRUSTSTORETYPE;
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
  private static final String TRUSTSTORE_PATH = "some_keystore_path";
  private static final String TRUSTSTORE_PASSWORD = "some_keystore_password";

  @Test
  public void testGetOkapiClientFactory() throws IllegalAccessException {
    Vertx vertx = Vertx.vertx();
    JsonObject config = new JsonObject()
      .put(SYS_OKAPI_URL, OKAPI_URL)
      .put(SYS_REQUEST_TIMEOUT_MS, REQ_TIMEOUT_MS)
      .put(FOLIO_CLIENT_TLS_ENABLED, false);
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
      .put(FOLIO_CLIENT_TLS_ENABLED, true)
      .put(FOLIO_CLIENT_TLS_TRUSTSTORETYPE, TRUSTSTORE_TYPE)
      .put(FOLIO_CLIENT_TLS_TRUSTSTOREPATH, TRUSTSTORE_PATH)
      .put(FOLIO_CLIENT_TLS_TRUSTSTOREPASSWORD, TRUSTSTORE_PASSWORD);
    OkapiClientFactory ocf = OkapiClientFactoryInitializer.createInstance(vertx, config);

    String okapiUrl = (String) FieldUtils.readDeclaredField(ocf, "okapiURL");
    Integer reqTimeoutMs = (Integer) FieldUtils.readDeclaredField(ocf, "reqTimeoutMs");
    KeyStoreOptions keyStoreOptions = (KeyStoreOptions) FieldUtils.readDeclaredField(ocf, "trustOptions", true);

    assertEquals(OKAPI_URL, okapiUrl);
    assertEquals(REQ_TIMEOUT_MS, reqTimeoutMs);
    assertEquals(TRUSTSTORE_TYPE, keyStoreOptions.getType());
    assertEquals(TRUSTSTORE_PATH, keyStoreOptions.getPath());
    assertEquals(TRUSTSTORE_PASSWORD, keyStoreOptions.getPassword());
    OkapiClient client = ocf.getOkapiClient("tenant");
    assertNotNull(client);
  }
}
