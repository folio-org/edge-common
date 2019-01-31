package org.folio.edge.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Base64;

import org.apache.log4j.Logger;
import org.folio.edge.core.model.ClientInfo;
import org.folio.edge.core.utils.ApiKeyUtils.MalformedApiKeyException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ApiKeyUtilsTest {
  public static final Logger logger = Logger.getLogger(ApiKeyUtilsTest.class);

  public static final String SALT_LEN = "10";
  public static final String TENANT = "diku";
  public static final String USERNAME = "diku";
  public static final String API_KEY = "Z0szc0RWZ3labF9kaWt1X2Rpa3U=";
  public static final String BAD_API_KEY = "bogus";
  public static final String DELIM = ApiKeyUtils.DELIM.pattern();

  public ApiKeyUtils utils;
  public ByteArrayOutputStream out;
  public ByteArrayOutputStream err;
  public PrintStream psOut;
  public PrintStream psErr;

  @Before
  public void setUp() throws Exception {
    utils = new ApiKeyUtils();

    out = new ByteArrayOutputStream();
    err = new ByteArrayOutputStream();
    psOut = new PrintStream(out);
    psErr = new PrintStream(err);

    System.setOut(psOut);
    System.setErr(psErr);
  }

  @After
  public void tearDown() throws Exception {
    err.close();
    out.close();
    psErr.close();
    psOut.close();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGenerateApiKeyNullSalt() throws Exception {
    logger.info("=== Test generate Api Key - null salt ===");
    ApiKeyUtils.generateApiKey(null, TENANT, USERNAME);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGenerateApiKeyNullTenant() throws Exception {
    logger.info("=== Test generate Api Key - null tenant ===");
    ApiKeyUtils.generateApiKey(10, null, USERNAME);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGenerateApiKeyNullUsername() throws Exception {
    logger.info("=== Test generate Api Key - null username ===");
    ApiKeyUtils.generateApiKey(10, TENANT, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGenerateApiKeyEmptySalt() throws Exception {
    logger.info("=== Test generate Api Key - Empty salt ===");
    ApiKeyUtils.generateApiKey("", TENANT, USERNAME);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGenerateApiKeyEmptyTenant() throws Exception {
    logger.info("=== Test generate Api Key - Empty tenant ===");
    ApiKeyUtils.generateApiKey(10, "", USERNAME);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGenerateApiKeyEmptyUsername() throws Exception {
    logger.info("=== Test generate Api Key - Empty username ===");
    ApiKeyUtils.generateApiKey(10, TENANT, "");
  }

  @Test(expected = MalformedApiKeyException.class)
  public void testParseApiKeyNullSalt() throws Exception {
    logger.info("=== Test parseApiKey - null salt ===");
    String apiKey = Base64.getUrlEncoder()
      .encodeToString(String.format("%s%s%s%s", DELIM, TENANT, DELIM, USERNAME).getBytes());
    ApiKeyUtils.parseApiKey(apiKey);
  }

  @Test(expected = MalformedApiKeyException.class)
  public void testParseApiKeyNullTenant() throws Exception {
    logger.info("=== Test parseApiKey - null tenant ===");
    String apiKey = Base64.getUrlEncoder()
      .encodeToString(String.format("%s%s%s%s", "abcde12345", DELIM, DELIM, USERNAME).getBytes());
    ApiKeyUtils.parseApiKey(apiKey);
  }

  @Test(expected = MalformedApiKeyException.class)
  public void testParseApiKeyNullUsername() throws Exception {
    logger.info("=== Test parseApiKey - null username ===");
    String apiKey = Base64.getUrlEncoder()
      .encodeToString(String.format("%s%s%s%s", "abcde12345", DELIM, TENANT, DELIM).getBytes());
    ApiKeyUtils.parseApiKey(apiKey);
  }

  @Test
  public void testGenerateSuccess() throws Exception {
    logger.info("=== Test Generate API Key - Success ===");

    String[] args = new String[] { "-g", "-s", SALT_LEN, "-t", TENANT, "-u", USERNAME };
    int status = utils.runMain(args);

    if (err.size() > 0) {
      logger.error(err.toString());
    }

    assertEquals(0, status);
    assertEquals(0, err.toByteArray().length);

    String generated = new String(out.toByteArray()).trim();
    logger.info("Generated: " + generated);

    ClientInfo info = ApiKeyUtils.parseApiKey(generated);

    assertEquals(Integer.parseInt(SALT_LEN), info.clientId.length());
    assertEquals(TENANT, info.tenantId);
    assertEquals(USERNAME, info.username);
  }

  @Test
  public void testGenerateNoTenant() throws Exception {
    logger.info("=== Test Generate API Key - missing tenant ===");

    String[] args = new String[] { "-g", "-s", SALT_LEN, "-u", USERNAME };
    int status = utils.runMain(args);

    if (err.size() > 0) {
      logger.error(err.toString());
    }

    assertEquals(2, status);
    assertNotEquals(0, err.size());
  }

  @Test
  public void testGenerateNoUsername() throws Exception {
    logger.info("=== Test Generate API Key - missing username ===");

    String[] args = new String[] { "-g", "-s", SALT_LEN, "-t", TENANT };
    int status = utils.runMain(args);

    if (err.size() > 0) {
      logger.error(err.toString());
    }

    assertEquals(2, status);
    assertNotEquals(0, err.size());
  }

  @Test
  public void testGenerateNoSaltLen() throws Exception {
    logger.info("=== Test Generate API Key - default Salt len ===");

    String[] args = new String[] { "-g", "-t", TENANT, "-u", USERNAME };
    int status = utils.runMain(args);

    if (err.size() > 0) {
      logger.error(err.toString());
    }

    assertEquals(0, status);
    assertEquals(0, err.toByteArray().length);

    String generated = new String(out.toByteArray()).trim();
    logger.info("Generated: " + generated);

    ClientInfo info = ApiKeyUtils.parseApiKey(generated);

    assertEquals(ApiKeyUtils.DEFAULT_SALT_LEN, info.clientId.length());
    assertEquals(TENANT, info.tenantId);
    assertEquals(USERNAME, info.username);
  }

  @Test
  public void testParseSuccess() throws Exception {
    logger.info("=== Test Parse API Key - Success ===");

    String[] args = new String[] { "-p", API_KEY };
    int status = utils.runMain(args);

    if (err.size() > 0) {
      logger.error(err.toString());
    }

    assertEquals(0, status);
    assertEquals(0, err.toByteArray().length);

    logger.info(out.toString());
  }

  @Test
  public void testParseFailure() throws Exception {
    logger.info("=== Test Parse API Key - Failure ===");

    String[] args = new String[] { "-p", BAD_API_KEY };
    int status = utils.runMain(args);

    if (err.size() > 0) {
      logger.error(err.toString());
    }

    assertEquals(3, status);
    assertNotEquals(0, err.toByteArray().length);

    logger.info(out.toString());
  }

  @Test
  public void testParseMissingKey() throws Exception {
    logger.info("=== Test Parse API Key - Missing Api Key ===");

    String[] args = new String[] { "-p" };
    int status = utils.runMain(args);

    if (err.size() > 0) {
      logger.error(err.toString());
    }

    assertEquals(2, status);
    assertNotEquals(0, err.toByteArray().length);

    logger.info(out.toString());
  }

  @Test
  public void testNoOpts() throws Exception {
    logger.info("=== Test No Options ===");

    String[] args = new String[] {};
    int status = utils.runMain(args);

    if (err.size() > 0) {
      logger.error(err.toString());
    }

    assertEquals(2, status);
    assertNotEquals(0, err.toByteArray().length);

    logger.info(out.toString());
  }
}
