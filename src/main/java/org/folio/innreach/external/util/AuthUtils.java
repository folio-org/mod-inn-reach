package org.folio.innreach.external.util;

public class AuthUtils {

  public static final String BEARER_AUTH_SCHEMA = "Bearer";
  public static final String BASIC_AUTH_SCHEMA = "Basic";

  public static String buildBearerAuthHeader(String authToken) {
    return String.format("%s %s", BEARER_AUTH_SCHEMA, authToken);
  }

  public static String buildBasicAuthHeader(String authToken) {
    return String.format("%s %s", BASIC_AUTH_SCHEMA, authToken);
  }
}
