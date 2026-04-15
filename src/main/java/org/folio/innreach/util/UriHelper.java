package org.folio.innreach.util;

import java.net.URI;
import lombok.experimental.UtilityClass;
import org.springframework.web.util.UriComponentsBuilder;

@UtilityClass
public class UriHelper {

  /**
   * Safely resolves path against base URI
   */
  public static URI buildUri(URI baseUri, String path) {
    // Ensure base has trailing slash
    var baseString = baseUri.toString();
    if (!baseString.endsWith("/")) {
      baseString = baseString + "/";
    }

    // Remove leading slash from path
    if (path.startsWith("/")) {
      path = path.substring(1);
    }

    return URI.create(baseString).resolve(path);
  }

  /**
   * Build URI with path variables
   */
  public static URI buildUri(URI baseUri, String pathTemplate, Object... variables) {
    var expandedPath = UriComponentsBuilder
      .fromPath(pathTemplate)
      .buildAndExpand(variables)
      .toUriString();

    return buildUri(baseUri, expandedPath);
  }
}
