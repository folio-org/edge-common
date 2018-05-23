package org.folio.edge.core.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.mockito.ArgumentCaptor;

public class TestUtils {

  public static int getPort() {
    return 1024 + (int) (Math.random() * 1000);
  }

  public static void assertLogMessage(Logger logger, int minTimes, int maxTimes, Level logLevel, String expectedMsg,
      Throwable t, Runnable func) {
    Appender appender = mock(Appender.class);

    try {
      logger.addAppender(appender);
      ArgumentCaptor<LoggingEvent> argument = ArgumentCaptor.forClass(LoggingEvent.class);

      func.run();

      verify(appender, atLeast(minTimes)).doAppend(argument.capture());
      verify(appender, atMost(maxTimes)).doAppend(argument.capture());

      if (logLevel != null)
        assertEquals(logLevel, argument.getValue().getLevel());

      if (expectedMsg != null)
        assertEquals(expectedMsg, argument.getValue().getMessage());

      if (t != null)
        assertEquals(t, argument.getValue().getThrowableInformation().getThrowable());
    } finally {
      if (logger != null && appender != null) {
        logger.removeAppender(appender);
      }
    }
  }
}
