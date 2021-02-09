package org.folio.edge.core;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
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
public class EdgeVerticleNoHttpServerTest {

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
  public void test1(TestContext context) {
    vertx.deployVerticle(new TestVerticleNoDefineRoutes()).onComplete(context.asyncAssertFailure(cause -> {
      context.assertEquals("defineRoutes must be defined for HTTP service", cause.getMessage());
    }));
  }
  public static class TestVerticleNoDefineRoutes extends EdgeVerticle {
  }

  @Test
  public void test2(TestContext context) {
    int serverPort = TestUtils.getPort();
    JsonObject jo = new JsonObject()
        .put(SYS_PORT, serverPort);

    final DeploymentOptions opt = new DeploymentOptions().setConfig(jo);
    TestVerticleTcpServer verticle = new TestVerticleTcpServer();
    vertx.deployVerticle(verticle, opt).onComplete(context.asyncAssertSuccess(res -> {
      context.assertEquals(verticle.port, serverPort);
    }));
  }

  public static class TestVerticleTcpServer extends EdgeVerticle {
    int port;
    @Override
    public Future<Void> startService() {
      port = config().getInteger(SYS_PORT);
      return vertx.createNetServer()
          .connectHandler(socket -> {
            socket.close();
          }).listen(port).mapEmpty();
    }
  }

}
