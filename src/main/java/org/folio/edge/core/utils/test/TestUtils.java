package org.folio.edge.core.utils.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.AbstractAppender;

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
      if (maxTries == 0 || isLocalPortFree(port)) {
        return port;
      }
      --maxTries;
    }
  }

  /**
   * Check a local TCP port.
   * @param port  the TCP port number, must be from 1 ... 65535
   * @return true if the port is free (unused), false if the port is already in use
   */
  static boolean isLocalPortFree(int port) {
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
    appender.stop();
    logger.removeAppender(appender);

    Assert.assertTrue(appender.events.size() >= minTimes);
    Assert.assertTrue(appender.events.size() <= maxTimes);

    if (logLevel != null) {
      Assert.assertTrue(!appender.events.isEmpty());
      // TODO .. appender.events.get(0).getLevel() always returns OFF
    }

    if (expectedMsg != null) {
      Assert.assertTrue(!appender.events.isEmpty());
      Assert.assertNotNull(expectedMsg, appender.events.get(0).getMessage());
      Assert.assertEquals(expectedMsg, appender.events.get(0).getMessage().getFormattedMessage());
    }
    if (t != null) {
      Assert.assertTrue(!appender.events.isEmpty());
      // TODO .. appender.events.get(0).getThrown() always return null
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
