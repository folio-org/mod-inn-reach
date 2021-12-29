package org.folio.innreach.mapper;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

@Component
public class MappingMethodsToEntity {

  public OffsetDateTime dateAsOffsetDateTime(Date date) {
    return date == null ? null : date.toInstant().atOffset(ZoneOffset.UTC);
  }

  public UUID stringAsUuid(String uuid) {
    return uuid == null ? null : UUID.fromString(uuid);
  }
}
