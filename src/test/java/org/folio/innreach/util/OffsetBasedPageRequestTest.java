package org.folio.innreach.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OffsetBasedPageRequestTest {
  @Test
  void shouldGetNextOffsetBasedPageRequest(){
    var pageRequest = new OffsetBasedPageRequest(2, 5);
    var nextPageRequest = pageRequest.next();

    assertEquals(7, nextPageRequest.getOffset());
    assertEquals(pageRequest.getPageSize(), nextPageRequest.getPageSize());
  }

  @Test
  void shouldReturnPreviousOrFirstRequest(){
    var pageRequest = new OffsetBasedPageRequest(5, 3);
    var previousRequest = pageRequest.previousOrFirst();

    assertEquals(2, previousRequest.getOffset());

    var firstRequest = previousRequest.previousOrFirst();

    assertEquals(0, firstRequest.getOffset());
  }
}
