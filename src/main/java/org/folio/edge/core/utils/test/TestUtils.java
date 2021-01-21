package org.folio.edge.core.utils.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.Assert;

public class TestUtils {

  private TestUtils() {

  }

  /**
   * return free TCP port.
   *
   * <p>An almost copy of the implementation in RMB.
   */
  public static int getPort() {
    int maxTries = 10000;
    while (true) {
        int port = ThreadLocalRandom.current().nextInt(49152 , 65535);
        if (isLocalPortFree(port)) {
            return port;
        }
        maxTries--;
        if (maxTries == 0){
          return 8081;
        }
    }
  }

  /**
   * Check a local TCP port.
   * @param port  the TCP port number, must be from 1 ... 65535
   * @return true if the port is free (unused), false if the port is already in use
   */
  private static boolean isLocalPortFree(int port) {
      try {
          new ServerSocket(port).close();
          return true;
      } catch (IOException e) {
          return false;
      }
  }

  public static void assertLogMessage(Logger logger, int minTimes, int maxTimes, Level logLevel,
                                      String expectedMsg, Throwable t, Runnable func) {
    assertLogMessage((org.apache.logging.log4j.core.Logger) logger, minTimes, maxTimes, logLevel, expectedMsg, t, func);
  }

  private static void assertLogMessage(org.apache.logging.log4j.core.Logger logger, int minTimes, int maxTimes, Level logLevel,
                                       String expectedMsg, Throwable t, Runnable func) {

    AppenderCapture appender = new AppenderCapture();

    logger.addAppender(appender);
    func.run();
    logger.removeAppender(appender);

    Assert.assertTrue(appender.events.size() >= minTimes);
    Assert.assertTrue(appender.events.size() <= maxTimes);

    // TODO .. the appender gets OFF so no comparison of logLevel for now

    if (expectedMsg != null) {
      Assert.assertTrue(!appender.events.isEmpty());
      Assert.assertNotNull(expectedMsg, appender.events.get(0).getMessage());
      Assert.assertEquals(expectedMsg, appender.events.get(0).getMessage().getFormattedMessage());
    }
    if (t != null) {
      Assert.assertTrue(!appender.events.isEmpty());
      Assert.assertNotNull(appender.events.get(0).getThreadName());
      Assert.assertEquals(t, appender.events.get(0).getThrown());
    }
  }


  private static class AppenderCapture extends AbstractAppender {
    protected List<LogEvent> events = new LinkedList<>();

    protected AppenderCapture() {
      super("MockedAppender", null, null, false, null);
      start();
    }

    @Override
    public void append(LogEvent event) {
      events.add(event);
    }

  }

}
