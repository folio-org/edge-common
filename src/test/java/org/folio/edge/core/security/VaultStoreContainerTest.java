package org.folio.edge.core.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Properties;
import org.folio.edge.core.security.SecureStore.NotFoundException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.vault.VaultContainer;

@RunWith(VertxUnitRunner.class)
public class VaultStoreContainerTest {

  @ClassRule
  public static VaultContainer<?> vaultContainer = new VaultContainer<>("vault:1.10.3")
        .withVaultToken("bee")
        .withSecretInVault("secret/diku", "diku_admin=password123");
  private static Properties properties = new Properties();
  private static Vertx vertx = Vertx.vertx();

  @BeforeClass
  public static void beforeClass() {
    vaultContainer.followOutput(out -> System.err.println(out.getUtf8String()));
    properties.setProperty("token", "bee");
    properties.setProperty("address", "http://" + getHostAndPort());
  }

  private static String getHostAndPort() {
    return vaultContainer.getHost() + ":" + vaultContainer.getMappedPort(8200);
  }

  @Test
  public void get() throws Throwable {
    assertThat(new VaultStore(properties).get("secret", "diku", "diku_admin"), is("password123"));
  }

  @Test
  public void getSucceededFuture(TestContext context) {
    new VaultStore(properties).get(vertx, "secret", "diku", "diku_admin")
    .onComplete(context.asyncAssertSuccess(value -> assertThat(value, is("password123"))));
  }

  @Test
  public void getFailedFuture(TestContext context) {
    new VaultStore(properties).get(vertx, "secret", "diku", "foo")
    .onComplete(context.asyncAssertFailure(e -> assertThat(e, is(instanceOf(NotFoundException.class)))));
  }

  // TODO Add test coverage for SSL/TLS configuration
}
