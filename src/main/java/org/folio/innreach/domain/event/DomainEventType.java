package org.folio.innreach.domain.event;

import com.fasterxml.jackson.annotation.JsonAlias;

public enum DomainEventType {
  @JsonAlias("CREATE") CREATED,
  @JsonAlias("UPDATE") UPDATED,
  @JsonAlias("DELETE") DELETED,
  @JsonAlias("DELETE_ALL") ALL_DELETED;
}
