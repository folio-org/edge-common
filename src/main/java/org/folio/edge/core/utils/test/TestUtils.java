package org.folio.edge.core.utils.test;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.Assert;

public class TestUtils {

  private static Random random = new Random(System.nanoTime());

  private TestUtils() {

  }

  public static int getPort() {
    return 1024 + random.nextInt(1000);
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
