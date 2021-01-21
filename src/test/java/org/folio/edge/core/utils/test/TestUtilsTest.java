package org.folio.edge.core.utils.test;

import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;

public class TestUtilsTest {
  private static final Logger logger = LogManager.getLogger(TestUtilsTest.class);

  @Test
  public void testGetPort() throws IOException {
    int port = TestUtils.getPort();
    assertTrue(port >= 49152);
    assertTrue(port <= 65535);
    new ServerSocket(port).close();
  }

  @Test
  public void testGetPort2() throws IOException {
    TestUtils.getPortReset();
    int port1 = TestUtils.getPort();
    ServerSocket serverSocket = new ServerSocket(port1);
    TestUtils.getPortReset();
    int port2 = TestUtils.getPort();
    Assert.assertNotEquals(port1, port2);
    serverSocket.close();
  }

  @Test
  public void testGetPortMaxTries0() throws IOException {
    int port = TestUtils.getPort(0);
    assertTrue(port >= 49152);
    assertTrue(port <= 65535);
  }

  @Test
  public void testIsLocalFreePort() throws IOException {
    int port = TestUtils.getPort();
    ServerSocket serverSocket = new ServerSocket(port);
    Assert.assertFalse(TestUtils.isLocalPortFree(port));
    serverSocket.close();
  }

  @Test(expected = AssertionError.class)
  public void testAssertLogMessageNone() {
    Logger log = LogManager.getLogger("testAssertLogMessageNone");
    String msg = "hello world";
    Level lvl = Level.INFO;
    TestUtils.assertLogMessage(log, 0, 1, lvl, msg, null, () -> {});
  }

  @Test
  public void testAssertLogMessageSingleMessage() {
    Logger log = LogManager.getLogger("testAssertLogMessageSingleMessage");
    String msg = "hello world";
    Level lvl = Level.INFO;
    TestUtils.assertLogMessage(log, 1, 1, lvl, msg, null, () -> logMessages(log, msg, 1, lvl));
  }

  @Test(expected = AssertionError.class)
  public void testAssertLogThrown() {
    Logger log = LogManager.getLogger("testAssertLogThrown");
    String msg = "hello world";
    Level lvl = Level.INFO;
    Throwable t = new IllegalArgumentException("il");
    TestUtils.assertLogMessage(log, 1, 1, lvl, "x", t, () -> {
      try {
        throw new IllegalStateException("il");
      } catch (Exception e) {
        log.error("x", e);
      }
    });
  }

  @Test
  public void testAssertLogMessageWrongLevel() {
    Logger log = LogManager.getLogger("testAssertLogMessageWrongLevel");
    String msg = "hello world";
    // logLevel not really checked, so this succeeds
    TestUtils.assertLogMessage(log, 1, 1, Level.ERROR, msg, null,
        () -> logMessages(log, msg, 1, Level.INFO));
  }

  @Test
  public void testAssertLogMessageNullLevel() {
    Logger log = LogManager.getLogger("testAssertLogMessageNullLevel");
    String msg = "hello world";
    TestUtils.assertLogMessage(log, 1, 1, null, msg, null,
        () -> logMessages(log, msg, 1, Level.INFO));
  }

  @Test(expected = AssertionError.class)
  public void testAssertLogMessageNoLevel() {
    Logger log = LogManager.getLogger("testAssertLogMessageNoLevel");
    String msg = "hello world";
    TestUtils.assertLogMessage(log, 0, 1, Level.INFO, msg, null, () -> {});
  }

  @Test
  public void testAssertLogMessageNullMessage() {
    Logger log = LogManager.getLogger("testAssertLogMessageNullMessage");
    Level lvl = Level.INFO;
    TestUtils.assertLogMessage(log, 1, 1, lvl, null, null,
        () -> logMessages(log, "goodbye blue monday", 1, lvl));
  }

  @Test(expected = AssertionError.class)
  public void testAssertLogMessageNoMessage() {
    Logger log = LogManager.getLogger("testAssertLogMessageNullMessage");
    String msg = "hello world";
    TestUtils.assertLogMessage(log, 0, 1, Level.INFO, msg, null, () -> {});
  }

  @Test(expected = AssertionError.class)
  public void testAssertLogMessageWrongMessage() {
    Logger log = LogManager.getLogger("testAssertLogMessageWrongMessage");
    String msg = "hello world";
    Level lvl = Level.INFO;
    TestUtils.assertLogMessage(log, 1, 1, lvl, msg, null,
        () -> logMessages(log, "goodbye blue monday", 1, lvl));
  }

  @Test(expected = AssertionError.class)
  public void testAssertLogMessageNothingLogged() {
    Logger log = LogManager.getLogger("testAssertLogMessageNothingLogged");
    String msg = "hello world";
    Level lvl = Level.INFO;
    TestUtils.assertLogMessage(log, 1, 1, lvl, msg, null, () -> {
    });
  }

  @Test(expected = AssertionError.class)
  public void testAssertLogMessageWithException() {
    String msg = "hello world";
    Level lvl = Level.WARN;
    TestUtils.assertLogMessage(logger, 1, 1, lvl, msg, new NullPointerException(),
        () -> logMessages(null, msg, 1, lvl));
  }

  @Test
  public void testAssertLogMessageExactCount() {
    Logger log = LogManager.getLogger("testAssertLogMessageExactCount");
    String msg = "hello world";
    Level lvl = Level.INFO;
    TestUtils.assertLogMessage(log, 5, 5, lvl, msg, null, () -> logMessages(log, msg, 5, lvl));
  }

  @Test
  public void testAssertLogMessageWithinRange() {
    Logger log = LogManager.getLogger("testAssertLogMessageWithinRange");
    String msg = "hello world";
    Level lvl = Level.INFO;
    TestUtils.assertLogMessage(log, 1, 10, lvl, msg, null, () -> logMessages(log, msg, 7, lvl));
  }

  @Test(expected = AssertionError.class)
  public void testAssertLogMessageOutsideRange() {
    Logger log = LogManager.getLogger("testAssertLogMessageWithinRange");
    String msg = "hello world";
    Level lvl = Level.INFO;
    TestUtils.assertLogMessage(log, 1, 5, lvl, msg, null, () -> logMessages(log, msg, 7, lvl));
  }

  private void logMessages(Logger log, String message, int times, Level level) {
    try {
      for (int i = 0; i < times; i++) {
        log.log(level, message);
      }
    } catch (Exception e) {
      logger.warn("Exception encountered", e);
    }
  }

}
