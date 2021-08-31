package org.folio.innreach.mapper;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class MappingMethods {

  public Date offsetDateTimeAsDate(OffsetDateTime offsetDateTime) {
    return offsetDateTime == null ? null : Date.from(offsetDateTime.toInstant());
  }

  public String uuidAsString(UUID uuid) {
    return uuid == null ? null : uuid.toString();
  }
}
