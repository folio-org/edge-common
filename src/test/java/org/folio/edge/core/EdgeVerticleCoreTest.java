package org.folio.edge.core;

import static org.folio.edge.core.Constants.SYS_PORT;
import static org.folio.edge.core.Constants.SYS_SECURE_STORE_PROP_FILE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.edge.core.utils.test.TestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class EdgeVerticleCoreTest {

  static Vertx vertx;

  @BeforeClass
  public static void setUpOnce(TestContext context) {
    vertx = Vertx.vertx();
  }

  @AfterClass
  public static void tearDownOnce(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testTcpServer(TestContext context) {
    int serverPort = TestUtils.getPort();
    JsonObject jo = new JsonObject()
        .put(SYS_PORT, serverPort);

    final DeploymentOptions opt = new DeploymentOptions().setConfig(jo);
    TestVerticleTcpServer verticle = new TestVerticleTcpServer();
    vertx.deployVerticle(verticle, opt).onComplete(context.asyncAssertSuccess(res ->
      context.assertEquals(verticle.port, serverPort)
    ));
  }

  public static class TestVerticleTcpServer extends EdgeVerticleCore {
    int port;
    @Override
    public void start(Promise<Void> promise) {
      Future.<Void>future(p -> super.start(p)).<Void>compose(res -> {
        port = config().getInteger(SYS_PORT);
        return vertx.createNetServer()
          .connectHandler(socket ->
            socket.close()
          ).listen(port).mapEmpty();
      }).onComplete(promise);
    }
  }

  // test getProperties failure handling
  // not doing it directly because we want to check that the Verticle reports failure
  @Test
  public void testGetPropertiesFailure1(TestContext context) {
    JsonObject jo = new JsonObject()
      .put(SYS_SECURE_STORE_PROP_FILE, "sx://foo.com");
    final DeploymentOptions opt = new DeploymentOptions().setConfig(jo);
    vertx.deployVerticle(new EdgeVerticleCore(), opt).onComplete(context.asyncAssertFailure(cause ->
      assertThat(cause.getMessage(), startsWith("Failed to load secure store properties: sx:/foo.com"))
    ));
  }

  // test getProperties failure handling
  // not doing it directly because we want to check that the Verticle reports failure
  @Test
  public void testGetPropertiesFailure2(TestContext context) {
    int serverPort = TestUtils.getPort();
    JsonObject jo = new JsonObject()
      .put(SYS_SECURE_STORE_PROP_FILE, "http://127.0.0.1:" + serverPort);
    final DeploymentOptions opt = new DeploymentOptions().setConfig(jo);
    vertx.deployVerticle(new EdgeVerticleCore(), opt).onComplete(context.asyncAssertFailure(cause ->
      assertThat(cause.getMessage(), containsString("Connection refused"))
    ));
  }

}
