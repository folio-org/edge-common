package org.folio.edge.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.vertx.core.json.JsonObject;

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
  public static final String SYS_API_KEY_SOURCES = "api_key_sources";
  public static final String SYS_RESPONSE_COMPRESSION = "response_compression";
  public static final String SYS_SSL_ENABLED = "ssl_enabled";
  public static final String SYS_KEYSTORE_PATH = "keystore_path";
  public static final String SYS_KEYSTORE_PASSWORD = "keystore_password";
  public static final String SYS_KEYSTORE_ALIAS = "keystore_alias";

  // Property names
  public static final String PROP_SECURE_STORE_TYPE = "secureStore.type";

  // Defaults
  public static final String DEFAULT_SECURE_STORE_TYPE = "ephemeral";
  public static final String DEFAULT_PORT = "8081";
  public static final String DEFAULT_LOG_LEVEL = "INFO";
  public static final int DEFAULT_REQUEST_TIMEOUT_MS = 30 * 1000; // ms
  public static final long DEFAULT_TOKEN_CACHE_TTL_MS = 60 * 60 * 1000L;
  public static final long DEFAULT_NULL_TOKEN_CACHE_TTL_MS = 30 * 1000L;
  public static final int DEFAULT_TOKEN_CACHE_CAPACITY = 100;
  public static final String DEFAULT_API_KEY_SOURCES = "PARAM,HEADER,PATH";
  public static final boolean DEFAULT_RESPONSE_COMPRESSION = false;
  public static final boolean DEFAULT_SSL_ENABLED = false;

  // Headers
  public static final String X_OKAPI_TENANT = "x-okapi-tenant";
  public static final String X_OKAPI_TOKEN = "x-okapi-token";
  public static final String HEADER_API_KEY = "Authorization";

  // Header Values
  public static final String APPLICATION_JSON = "application/json";
  public static final String APPLICATION_XML = "application/xml";
  public static final String TEXT_PLAIN = "text/plain";
  public static final String TEXT_XML = "text/xml";
  public static final String JSON_OR_TEXT = APPLICATION_JSON + ", " + TEXT_PLAIN;
  public static final String XML_OR_TEXT = APPLICATION_XML + ", " + TEXT_PLAIN;

  // Param names
  public static final String PARAM_API_KEY = "apikey";

  // Path components
  public static final String PATH_API_KEY = "apiKeyPath";

  // Response messages
  public static final String MSG_ACCESS_DENIED = "Access Denied";
  public static final String MSG_INTERNAL_SERVER_ERROR = "Internal Server Error";
  public static final String MSG_REQUEST_TIMEOUT = "Request to FOLIO timed out";
  public static final String MSG_NOT_IMPLEMENTED = "Not Implemented";
  public static final String MSG_INVALID_API_KEY = "Invalid API Key";

  // Misc
  public static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000L;

  // Seed the default deployment options with the system env variables or
  // the defaults defined above.
  public static final JsonObject DEFAULT_DEPLOYMENT_OPTIONS;
  static {
    final Map<String, Object> defaultMap = new HashMap<>();

    defaultMap.put(SYS_PORT,
        Integer.parseInt(System.getProperty(SYS_PORT, DEFAULT_PORT)));
    defaultMap.put(SYS_LOG_LEVEL,
        System.getProperty(SYS_LOG_LEVEL, DEFAULT_LOG_LEVEL));
    defaultMap.put(SYS_API_KEY_SOURCES,
        System.getProperty(SYS_API_KEY_SOURCES, DEFAULT_API_KEY_SOURCES));
    defaultMap.put(SYS_REQUEST_TIMEOUT_MS,
        Long.parseLong(System.getProperty(SYS_REQUEST_TIMEOUT_MS,
            Long.toString(DEFAULT_REQUEST_TIMEOUT_MS))));
    defaultMap.put(SYS_TOKEN_CACHE_TTL_MS,
        Long.parseLong(System.getProperty(SYS_TOKEN_CACHE_TTL_MS,
            Long.toString(DEFAULT_TOKEN_CACHE_TTL_MS))));
    defaultMap.put(SYS_NULL_TOKEN_CACHE_TTL_MS,
        Long.parseLong(System.getProperty(SYS_NULL_TOKEN_CACHE_TTL_MS,
            Long.toString(DEFAULT_NULL_TOKEN_CACHE_TTL_MS))));
    defaultMap.put(SYS_TOKEN_CACHE_CAPACITY,
        Integer.parseInt(System.getProperty(SYS_TOKEN_CACHE_CAPACITY,
            Integer.toString(DEFAULT_TOKEN_CACHE_CAPACITY))));
    defaultMap.put(SYS_SECURE_STORE_TYPE,
        System.getProperty(SYS_SECURE_STORE_TYPE, DEFAULT_SECURE_STORE_TYPE));
    defaultMap.put(SYS_RESPONSE_COMPRESSION,
        Boolean.parseBoolean(System.getProperty(SYS_RESPONSE_COMPRESSION,
            Boolean.toString(DEFAULT_RESPONSE_COMPRESSION))));
    defaultMap.put(SYS_SSL_ENABLED,
        Boolean.parseBoolean(System.getProperty(SYS_SSL_ENABLED,
          Boolean.toString(DEFAULT_SSL_ENABLED))));
    defaultMap.put(SYS_SECURE_STORE_PROP_FILE,
        System.getProperty(SYS_SECURE_STORE_PROP_FILE));
    defaultMap.put(SYS_OKAPI_URL,
        System.getProperty(SYS_OKAPI_URL));

    DEFAULT_DEPLOYMENT_OPTIONS = new JsonObject(Collections.unmodifiableMap(defaultMap));
  }
}
