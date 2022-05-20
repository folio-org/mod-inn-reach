package org.folio.innreach.mapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class MappingMethods {

  public Date offsetDateTimeAsDate(OffsetDateTime offsetDateTime) {
    return offsetDateTime == null ? null : Date.from(offsetDateTime.toInstant());
  }

  public OffsetDateTime dateAsOffsetDateTime(Date date) {
    // assuming date is in UTC
    return date == null ? null : date.toInstant().atOffset(ZoneOffset.UTC);
  }

  public String uuidAsString(UUID uuid) {
    return uuid == null ? null : uuid.toString();
  }

}
