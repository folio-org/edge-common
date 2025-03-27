package org.folio.edge.core.utils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

import org.folio.edge.core.model.ClientInfo;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import io.vertx.core.json.JsonObject;

public class ApiKeyUtils {

  public static final String ALPHANUMERIC = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static final Random RANDOM = new SecureRandom();
  /** 17 characters give 100 bits of entropy */
  public static final int DEFAULT_SALT_LEN = 17;

  @Option(name = "-p",
          usage = "parse an API Key",
          forbids = "-g")
  private String keyToParse;

  @Option(name = "-g",
          usage = "generate an API Key",
          forbids = "-p",
          depends = { "-t", "-u" })
  private boolean generate;

  @Option(name = "-s",
          aliases = "--salt-len",
          usage = "the number of salt characters",
          depends = "-g")
  private int saltLen = DEFAULT_SALT_LEN;

  @Option(name = "-t",
          aliases = "--tenant-id",
          usage = "the tenant's ID",
          depends = "-g")
  private String tenantId;

  @Option(name = "-u",
          aliases = "--username",
          usage = "the tenant's institutional user's username",
          depends = "-g")
  private String username;

  public static String generateSalt(int length) {
    StringBuilder buf = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      buf.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
    }
    return buf.toString();
  }

  public static String generateApiKey(String salt, String tenantId, String username) {
    if (salt == null || salt.isEmpty()) {
      throw new IllegalArgumentException("ClientID/Salt cannot be null");
    }

    if (tenantId == null || tenantId.isEmpty()) {
      throw new IllegalArgumentException("TenantID cannot be null");
    }

    if (username == null || username.isEmpty()) {
      throw new IllegalArgumentException("Username cannot be null");
    }

    ClientInfo ci = new ClientInfo(salt, tenantId, username);
    return Base64.getUrlEncoder().encodeToString(JsonObject.mapFrom(ci).encode().getBytes());
  }

  public static String generateApiKey(int saltLen, String tenantId, String username) {
    return generateApiKey(generateSalt(saltLen), tenantId, username);
  }

  public static ClientInfo parseApiKey(String apiKey) throws MalformedApiKeyException {
    ClientInfo ret = null;
    try {
      String decoded = new String(Base64.getUrlDecoder().decode(apiKey.getBytes()));
      JsonObject json = new JsonObject(decoded);

      ret = json.mapTo(ClientInfo.class);
    } catch (Exception e) {
      throw new MalformedApiKeyException("Failed to parse", e);
    }

    if (ret.salt == null || ret.salt.isEmpty()) {
      throw new MalformedApiKeyException("Null/Empty Salt");
    }

    if (ret.tenantId == null || ret.tenantId.isEmpty()) {
      throw new MalformedApiKeyException("Null/Empty Tenant");
    }

    if (ret.username == null || ret.username.isEmpty()) {
      throw new MalformedApiKeyException("Null/Empty Username");
    }

    return ret;
  }

  public static void main(String[] args) {
    ApiKeyUtils utils = new ApiKeyUtils();
    System.exit(utils.runMain(args));
  }

  @SuppressWarnings({ "squid:S106", "squid:S1148" })
  public int runMain(String[] args) {
    CmdLineParser parser = new CmdLineParser(this);

    try {
      // parse the arguments.
      parser.parseArgument(args);

      if (generate) {
        System.out.println(generateApiKey(saltLen, tenantId, username));
        return 0;
      } else if (keyToParse != null && !keyToParse.isEmpty()) {
        ClientInfo info = parseApiKey(keyToParse);
        System.out.println("Salt: " + info.salt);
        System.out.println("Tenant ID: " + info.tenantId);
        System.out.println("Username: " + info.username);
        return 0;
      } else {
        throw new CmdLineException(parser, "Either -g or -p need to be specified", new Exception());
      }

    } catch (CmdLineException e) {
      // if there's a problem in the command line,
      // you'll get this exception. this will report
      // an error message.
      System.err.println(e.getMessage());
      System.err.println("Usage: ApiKeyUtils [options]");
      // print the list of available options
      parser.printUsage(System.err);
      System.err.println();
      return 2;
    } catch (MalformedApiKeyException e) {
      System.err.println("Failed to parse API Key!");
      e.printStackTrace();
      return 3;
    }
  }

  public static class MalformedApiKeyException extends Exception {

    private static final long serialVersionUID = 7852873967223950947L;
    public static final String MSG = "Malformed API Key";

    public MalformedApiKeyException(String msg, Throwable t) {
      super(MSG + ": " + msg, t);
    }

    public MalformedApiKeyException(String msg) {
      super(MSG + ": " + msg);
    }
  }
}
