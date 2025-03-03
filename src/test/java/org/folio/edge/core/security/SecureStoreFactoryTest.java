package org.folio.edge.core.security;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.Properties;

import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkClientException;

public class SecureStoreFactoryTest {

  public static final Class<? extends SecureStore> DEFAULT_SS_CLASS = EphemeralStore.class;

  @Test
  public void testGetSecureStoreKnownTypes()
      throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
    Class<?>[] stores = new Class<?>[] {
        AwsParamStore.class,
        EphemeralStore.class,
        VaultStore.class
    };

    SecureStore actual;

    for (Class<?> clazz : stores) {
      Properties props = new Properties();

      if (clazz.equals(AwsParamStore.class)) {
        props.put(AwsParamStore.PROP_REGION, "us-east-1");
      }

      actual = SecureStoreFactory.getSecureStore((String) clazz.getField("TYPE").get(null), props);
      assertThat(actual, instanceOf(clazz));
    }
  }

  @Test
  public void testAwsParamStoreNull() {
    assertThrows(SdkClientException.class, () -> SecureStoreFactory.getSecureStore(AwsParamStore.TYPE, null));
  }

  @Test
  public void testVaultStoreNull() {
    assertThrows(NullPointerException.class, () -> SecureStoreFactory.getSecureStore(VaultStore.TYPE, null));
  }

  @Test
  public void testEphemeralStoreNull() {
    assertThat(SecureStoreFactory.getSecureStore(EphemeralStore.TYPE, null), instanceOf(EphemeralStore.class));
  }

  @Test
  public void testGetSecureStoreDefaultType() {
    SecureStore actual;

    // unknown type
    actual = SecureStoreFactory.getSecureStore("foo", new Properties());
    assertThat(actual, instanceOf(DEFAULT_SS_CLASS));
    actual = SecureStoreFactory.getSecureStore("foo", null);
    assertThat(actual, instanceOf(DEFAULT_SS_CLASS));

    // null type
    actual = SecureStoreFactory.getSecureStore(null, new Properties());
    assertThat(actual, instanceOf(DEFAULT_SS_CLASS));
    actual = SecureStoreFactory.getSecureStore(null, null);
    assertThat(actual, instanceOf(DEFAULT_SS_CLASS));
  }
}
