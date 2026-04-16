package org.folio.edge.core.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Properties;
import org.folio.edge.core.security.SecureStore.NotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

@Testcontainers
@ExtendWith(VertxExtension.class)
public class VaultStoreContainerTest {

  @Container
  public static VaultContainer<?> vaultContainer = new VaultContainer<>("hashicorp/vault:1.21")
        .withVaultToken("bee")
        .withInitCommand("kv put secret/diku diku_admin=password123");
  private static Properties properties = new Properties();
  private static Vertx vertx = Vertx.vertx();

  @BeforeAll
  public static void beforeClass() {
    vaultContainer.followOutput(out -> System.err.println(out.getUtf8String()));
    properties.setProperty("token", "bee");
    properties.setProperty("address", vaultContainer.getHttpHostAddress());
  }

  @Test
  public void get() throws Throwable {
    assertThat(new VaultStore(properties).get("secret", "diku", "diku_admin"), is("password123"));
  }

  @Test
  public void getSucceededFuture(VertxTestContext vtc) {
    new VaultStore(properties).get(vertx, "secret", "diku", "diku_admin")
    .onComplete(vtc.succeeding(value -> {
      assertThat(value, is("password123"));
      vtc.completeNow();
    }));
  }

  @Test
  public void getFailedFuture(VertxTestContext vtc) {
    new VaultStore(properties).get(vertx, "secret", "diku", "foo")
    .onComplete(vtc.failing(e -> {
      assertThat(e, is(instanceOf(NotFoundException.class)));
      vtc.completeNow();
    }));
  }

  // TODO Add test coverage for SSL/TLS configuration
}
