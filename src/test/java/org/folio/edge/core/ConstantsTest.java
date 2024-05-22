package org.folio.edge.core;

import io.vertx.core.json.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ConstantsTest {

  @Before
  public void teardown() {
    System.setProperty(Constants.SYS_HTTP_SERVER_SSL_ENABLED, "true");
    System.setProperty(Constants.SYS_HTTP_SERVER_KEYSTORE_TYPE, "BCFKS");
    System.setProperty(Constants.SYS_HTTP_SERVER_KEYSTORE_PROVIDER, "BCFIPS");
    System.setProperty(Constants.SYS_HTTP_SERVER_KEYSTORE_PATH, "test.keystore 1.bcfks");
    System.setProperty(Constants.SYS_HTTP_SERVER_KEYSTORE_PASSWORD, "SecretPassword");
    System.setProperty(Constants.SYS_HTTP_SERVER_KEY_ALIAS, "localhost");
    System.setProperty(Constants.SYS_HTTP_SERVER_KEY_ALIAS_PASSWORD, "SecretPassword");
    System.setProperty(Constants.SYS_WEB_CLIENT_SSL_ENABLED, "true");
    System.setProperty(Constants.SYS_WEB_CLIENT_TRUSTSTORE_TYPE, "BCFKS");
    System.setProperty(Constants.SYS_WEB_CLIENT_TRUSTSTORE_PROVIDER, "BCFIPS");
    System.setProperty(Constants.SYS_WEB_CLIENT_TRUSTSTORE_PATH, "test.store 1.bcfks");
    System.setProperty(Constants.SYS_WEB_CLIENT_TRUSTSTORE_PASSWORD, "SecretPassword");
    System.setProperty(Constants.SYS_WEB_CLIENT_KEY_ALIAS, "localhost");
    System.setProperty(Constants.SYS_WEB_CLIENT_KEY_ALIAS_PASSWORD, "SecretPassword");
  }

  @After
  public void after() {
    System.clearProperty(Constants.SYS_HTTP_SERVER_SSL_ENABLED);
    System.clearProperty(Constants.SYS_HTTP_SERVER_KEYSTORE_TYPE);
    System.clearProperty(Constants.SYS_HTTP_SERVER_KEYSTORE_PROVIDER);
    System.clearProperty(Constants.SYS_HTTP_SERVER_KEYSTORE_PATH);
    System.clearProperty(Constants.SYS_HTTP_SERVER_KEYSTORE_PASSWORD);
    System.clearProperty(Constants.SYS_HTTP_SERVER_KEY_ALIAS);
    System.clearProperty(Constants.SYS_HTTP_SERVER_KEY_ALIAS_PASSWORD);

    System.clearProperty(Constants.SYS_WEB_CLIENT_SSL_ENABLED);
    System.clearProperty(Constants.SYS_WEB_CLIENT_TRUSTSTORE_TYPE);
    System.clearProperty(Constants.SYS_WEB_CLIENT_TRUSTSTORE_PROVIDER);
    System.clearProperty(Constants.SYS_WEB_CLIENT_TRUSTSTORE_PATH);
    System.clearProperty(Constants.SYS_WEB_CLIENT_TRUSTSTORE_PASSWORD);
    System.clearProperty(Constants.SYS_WEB_CLIENT_KEY_ALIAS);
    System.clearProperty(Constants.SYS_WEB_CLIENT_KEY_ALIAS_PASSWORD);
  }

  @Test(expected = UnsupportedOperationException.class)
  public final void testUnmodifiableDepoymentOptions() {
    Constants.DEFAULT_DEPLOYMENT_OPTIONS.put("test", "invalid");
  }

  @Test
  public void testSslServerDeploymentOptions() {
    JsonObject config = Constants.DEFAULT_DEPLOYMENT_OPTIONS.copy();

    assertThat(config.getString(Constants.SYS_HTTP_SERVER_SSL_ENABLED),true);
    assertThat(config.getString(Constants.SYS_HTTP_SERVER_KEYSTORE_TYPE), is("BCFKS"));
    assertThat(config.getString(Constants.SYS_HTTP_SERVER_KEYSTORE_PROVIDER), is("BCFIPS"));
    assertThat(config.getString(Constants.SYS_HTTP_SERVER_KEYSTORE_PATH), is("test.keystore 1.bcfks"));
    assertThat(config.getString(Constants.SYS_HTTP_SERVER_KEYSTORE_PASSWORD), is("SecretPassword"));
    assertThat(config.getString(Constants.SYS_HTTP_SERVER_KEY_ALIAS), is("localhost"));
    assertThat(config.getString(Constants.SYS_HTTP_SERVER_KEY_ALIAS_PASSWORD), is("SecretPassword"));
  }

  @Test
  public void testSslClientDeploymentOptions() {
    JsonObject config = Constants.DEFAULT_DEPLOYMENT_OPTIONS.copy();

    assertThat(config.getString(Constants.SYS_WEB_CLIENT_SSL_ENABLED),true);
    assertThat(config.getString(Constants.SYS_WEB_CLIENT_TRUSTSTORE_TYPE), is("BCFKS"));
    assertThat(config.getString(Constants.SYS_WEB_CLIENT_TRUSTSTORE_PROVIDER), is("BCFIPS"));
    assertThat(config.getString(Constants.SYS_WEB_CLIENT_TRUSTSTORE_PATH), is("test.store 1.bcfks"));
    assertThat(config.getString(Constants.SYS_WEB_CLIENT_TRUSTSTORE_PASSWORD), is("SecretPassword"));
    assertThat(config.getString(Constants.SYS_WEB_CLIENT_KEY_ALIAS), is("localhost"));
    assertThat(config.getString(Constants.SYS_WEB_CLIENT_KEY_ALIAS_PASSWORD), is("SecretPassword"));
  }
}
