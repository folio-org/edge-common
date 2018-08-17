package org.folio.edge.core;

import static org.folio.edge.core.Constants.HEADER_API_KEY;
import static org.folio.edge.core.Constants.PARAM_API_KEY;
import static org.folio.edge.core.Constants.PATH_API_KEY;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.ext.web.RoutingContext;

public class ApiKeyHelper {

  public static final Pattern AUTH_TYPE = Pattern.compile("(?i).*apikey (\\w*).*");
  public static final Pattern COMMA = Pattern.compile(",");

  public final List<ApiKeySource> sources;

  public ApiKeyHelper(String apiKeySources) {
    sources = new ArrayList<>();

    if (apiKeySources == null) {
      throw new IllegalArgumentException("No API Key source specified");
    }
    for (String source : COMMA.split(apiKeySources)) {
      sources.add(ApiKeySource.valueOf(source));
    }
  }

  public String getApiKey(RoutingContext ctx) {
    String apiKey = null;
    for (ApiKeySource source : sources) {
      switch (source) {
      case PARAM:
        apiKey = getFromParam(ctx);
        break;

      case HEADER:
        apiKey = getFromHeader(ctx);
        break;

      case PATH:
        apiKey = getFromPath(ctx);
        break;
      }

      if (apiKey != null) {
        break;
      }
    }
    return apiKey;
  }

  public String getFromParam(RoutingContext ctx) {
    return ctx.request().getParam(PARAM_API_KEY);
  }

  public String getFromHeader(RoutingContext ctx) {
    String full = ctx.request().getHeader(HEADER_API_KEY);

    if (full == null || full.isEmpty()) {
      return null;
    }

    Matcher matcher = AUTH_TYPE.matcher(full);
    if (matcher.matches()) {
      return matcher.group(1);
    } else {
      return full;
    }
  }

  public String getFromPath(RoutingContext ctx) {
    return ctx.request().getParam(PATH_API_KEY);
  }

  public enum ApiKeySource {
    PARAM, HEADER, PATH
  }

}
