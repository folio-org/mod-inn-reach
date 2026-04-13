package org.folio.innreach.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import org.junit.jupiter.api.Test;

class UriHelperTest {

  @Test
  void buildUri_withRelativePath_appendsToBase() {
    var result = UriHelper.buildUri(URI.create("https://example.com/base/"), "items/123");

    assertEquals(URI.create("https://example.com/base/items/123"), result);
  }

  @Test
  void buildUri_withoutBaseTrailingSlash_addsSeparator() {
    var result = UriHelper.buildUri(URI.create("https://example.com/base"), "items/123");

    assertEquals(URI.create("https://example.com/base/items/123"), result);
  }

  @Test
  void buildUri_withLeadingSlashPath_ignoresLeadingSlash() {
    var result = UriHelper.buildUri(URI.create("https://example.com/base"), "/items/123");

    assertEquals(URI.create("https://example.com/base/items/123"), result);
  }

  @Test
  void buildUri_withBaseContainingPath_resolvesAgainstExistingBasePath() {
    var result = UriHelper.buildUri(URI.create("https://example.com/api/v1"), "requests");

    assertEquals(URI.create("https://example.com/api/v1/requests"), result);
  }

  @Test
  void buildUri_withPathTemplateAndSingleVariable_expandsTemplate() {
    var result = UriHelper.buildUri(URI.create("https://example.com/base"), "items/{id}", 42);

    assertEquals(URI.create("https://example.com/base/items/42"), result);
  }

  @Test
  void buildUri_withPathTemplateAndMultipleVariables_expandsInOrder() {
    var result = UriHelper.buildUri(URI.create("https://example.com/base"), "libraries/{libraryId}/items/{itemId}", "abc", "123");

    assertEquals(URI.create("https://example.com/base/libraries/abc/items/123"), result);
  }

  @Test
  void buildUri_withNullBaseUri_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> UriHelper.buildUri(null, "items/123"));
  }

  @Test
  void buildUri_withNullPath_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> UriHelper.buildUri(URI.create("https://example.com/base"), null));
  }

  @Test
  void buildUri_withMissingTemplateVariable_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
      () -> UriHelper.buildUri(URI.create("https://example.com/base"), "items/{id}/{holdingId}", 1));
  }
}

