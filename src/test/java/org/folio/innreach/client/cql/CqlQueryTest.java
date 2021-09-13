package org.folio.innreach.client.cql;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import static org.folio.innreach.client.cql.CqlQuery.exactMatchAny;

import org.junit.jupiter.api.Test;

class CqlQueryTest {

  @Test
  void exactMatchAny_positive() {
    assertThat(exactMatchAny("id", asList("id1", "id2")))
      .hasToString("id==(\"id1\" or \"id2\")");
  }

  @Test
  void exactMatchAny_shouldFilterOutEmptyString() {
    assertThat(exactMatchAny("id", asList("id1", null, "")))
      .hasToString("id==(\"id1\")");
  }

}
