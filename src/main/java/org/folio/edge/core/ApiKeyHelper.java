package org.folio.edge.core;

import static org.folio.edge.core.Constants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import io.vertx.ext.web.RoutingContext;

public class ApiKeyHelper {

  public static final Pattern COMMA = Pattern.compile(",");

  public final List<ApiKeySource> sources;

  public ApiKeyHelper(String apiKeySources) {
    sources = new ArrayList<>();
    for (String source : COMMA.split(apiKeySources)) {
      sources.add(ApiKeySource.valueOf(source));
    }
  }

  public String getApiKey(RoutingContext ctx) {
    String apiKey = null;
    for (ApiKeySource source : sources) {
      switch (source) {
      case PARAM:
        apiKey = ctx.request().getParam(PARAM_API_KEY);
        break;

      case HEADER:
        apiKey = ctx.request().getHeader(HEADER_API_KEY);
        break;

      case PATH:
        apiKey = ctx.request().getParam(PATH_API_KEY);
        break;
      }
      
      if (apiKey != null) {
        break;
      }
    }
    return apiKey;
  }

  public enum ApiKeySource {
    PARAM, HEADER, PATH
  }

}
