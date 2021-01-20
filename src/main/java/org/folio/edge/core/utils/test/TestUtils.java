package org.folio.edge.core.utils.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Random;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.Level;
import org.mockito.ArgumentCaptor;

public class TestUtils {

  private static Random random = new Random(System.nanoTime());

  private TestUtils() {

  }

  public static int getPort() {
    return 1024 + random.nextInt(1000);
  }

  public static void assertLogMessage(org.apache.logging.log4j.Logger logger, int minTimes, int maxTimes, Level logLevel, String expectedMsg,
                                      Throwable t, Runnable func) {
    assertLogMessage((org.apache.logging.log4j.core.Logger) logger, minTimes, maxTimes, logLevel, expectedMsg, t, func);
  }

  public static void assertLogMessage(org.apache.logging.log4j.core.Logger logger, int minTimes, int maxTimes, Level logLevel, String expectedMsg,
                                      Throwable t, Runnable func) {

    Appender appender = mock(Appender.class);

    try {
      logger.addAppender(appender);
      ArgumentCaptor<LogEvent> argument = ArgumentCaptor.forClass(LogEvent.class);

      func.run();

      verify(appender, atLeast(minTimes)).append(argument.capture());
      verify(appender, atMost(maxTimes)).append(argument.capture());

      if (logLevel != null)
        assertEquals(logLevel, argument.getValue().getLevel());

      if (expectedMsg != null)
        assertEquals(expectedMsg, argument.getValue().getMessage());

      if (t != null) {
        assertNotNull(argument.getValue().getThreadName());
        assertEquals(t, argument.getValue().getThrown());
      }
    } finally {
      logger.removeAppender(appender);
    }
  }
}
