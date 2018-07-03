package org.folio.edge.core;

public class Constants {

  private Constants() {

  }

  // System Properties
  public static final String SYS_SECURE_STORE_PROP_FILE = "secure_store_props";
  public static final String SYS_SECURE_STORE_TYPE = "secure_store";
  public static final String SYS_OKAPI_URL = "okapi_url";
  public static final String SYS_PORT = "port";
  public static final String SYS_TOKEN_CACHE_TTL_MS = "token_cache_ttl_ms";
  public static final String SYS_NULL_TOKEN_CACHE_TTL_MS = "null_token_cache_ttl_ms";
  public static final String SYS_TOKEN_CACHE_CAPACITY = "token_cache_capacity";
  public static final String SYS_LOG_LEVEL = "log_level";
  public static final String SYS_REQUEST_TIMEOUT_MS = "request_timeout_ms";

  // Property names
  public static final String PROP_SECURE_STORE_TYPE = "secureStore.type";

  // Defaults
  public static final String DEFAULT_SECURE_STORE_TYPE = "ephemeral";
  public static final String DEFAULT_PORT = "8081";
  public static final String DEFAULT_LOG_LEVEL = "INFO";
  public static final long DEFAULT_REQUEST_TIMEOUT_MS = 30 * 1000L; // ms
  public static final long DEFAULT_TOKEN_CACHE_TTL_MS = 60 * 60 * 1000L;
  public static final long DEFAULT_NULL_TOKEN_CACHE_TTL_MS = 30 * 1000L;
  public static final int DEFAULT_TOKEN_CACHE_CAPACITY = 100;

  // Headers
  public static final String X_OKAPI_TENANT = "x-okapi-tenant";
  public static final String X_OKAPI_TOKEN = "x-okapi-token";

  // Header Values
  public static final String APPLICATION_JSON = "application/json";
  public static final String APPLICATION_XML = "application/xml";
  public static final String TEXT_PLAIN = "text/plain";
  public static final String JSON_OR_TEXT = APPLICATION_JSON + ", " + TEXT_PLAIN;

  // Param names
  public static final String PARAM_API_KEY = "apikey";

  // Response messages
  public static final String MSG_ACCESS_DENIED = "Access Denied";
  public static final String MSG_INTERNAL_SERVER_ERROR = "Internal Server Error";
  public static final String MSG_REQUEST_TIMEOUT = "Request to FOLIO timed out";
  public static final String MSG_NOT_IMPLEMENTED = "Not Implemented";

  // Misc
  public static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000L;
}
