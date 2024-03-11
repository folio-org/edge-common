package org.folio.edge.core.utils.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.Assert;

public class TestUtils {

  private TestUtils() {

  }

  private static final int PORT_MIN = 49152;
  private static final int PORT_MAX = 65534;
  private static final AtomicInteger cyclicPort = new AtomicInteger(PORT_MAX);

  /**
   * return free TCP port.
   *
   * <p>An based on implementation in RMB.
   * The purpose is to return a free port. We try to test for available ports that differ in
   * each iteration so avoid the case where it takes a little while for a returned value
   * to be listened to .. If not, we could end up returning the same port twice. RMB uses
   * random generator to do that (but a random generator may return value anyway ... or
   * a port that is one of the earlier ports.. For this reason, it's best to attempt in
   * a cyclic fashion.
   */
  public static int getPort() {
    return getPort(10000);
  }

  static int getPort(int maxTries) {
    while (true) {
      int port = cyclicPort.incrementAndGet();
      if (port > PORT_MAX) {
        cyclicPort.set(PORT_MIN);
      }
      if (maxTries == 0 || isLocalPortFree(port)) {
        return port;
      }
      --maxTries;
    }
  }

  static void getPortReset() {
    cyclicPort.set(PORT_MAX);
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

  /**
   * Unit test for logged content.
   *
   * @param logger that is used in test code
   * @param minTimes minimum number of log events
   * @param maxTimes maximum number of log events
   * @param logLevel expected level for first message
   * @param expectedMsg expected message for first message
   * @param t expected Throwable (not working, pass null always)
   * @param func function to execute which presumably logs
   */
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

    Assert.assertNull(t);

    if (logLevel != null) {
      Assert.assertFalse(appender.events.isEmpty());
      Level gotLevel = appender.events.get(0).getLevel();
      Assert.assertEquals("log level", logLevel, gotLevel);
    }

    if (expectedMsg != null) {
      Assert.assertFalse(appender.events.isEmpty());
      Assert.assertNotNull(expectedMsg, appender.events.get(0).getMessage());
      Assert.assertEquals(expectedMsg, appender.events.get(0).getMessage().getFormattedMessage());
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
      events.add(event.toImmutable());
    }

  }

}
