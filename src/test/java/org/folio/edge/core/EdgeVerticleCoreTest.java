package org.folio.edge.core;

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

import static org.folio.edge.core.Constants.SYS_PORT;

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
  public void test(TestContext context) {
    int serverPort = TestUtils.getPort();
    JsonObject jo = new JsonObject()
        .put(SYS_PORT, serverPort);

    final DeploymentOptions opt = new DeploymentOptions().setConfig(jo);
    TestVerticleTcpServer verticle = new TestVerticleTcpServer();
    vertx.deployVerticle(verticle, opt).onComplete(context.asyncAssertSuccess(res -> {
      context.assertEquals(verticle.port, serverPort);
    }));
  }

  public static class TestVerticleTcpServer extends EdgeVerticleCore {
    int port;
    @Override
    public void start(Promise<Void> promise) {
      Future.<Void>future(p -> super.start(p)).<Void>compose(res -> {
        port = config().getInteger(SYS_PORT);
        return vertx.createNetServer()
          .connectHandler(socket -> {
            socket.close();
          }).listen(port).mapEmpty();
      }).onComplete(promise);
    }
  }
}
