package org.folio.innreach.mapper;

import java.time.OffsetDateTime;
import java.util.Date;

import org.springframework.stereotype.Component;

@Component
public class DateMapper {

  public Date offsetDateTimeAsDate(OffsetDateTime offsetDateTime) {
    return offsetDateTime == null ? null : Date.from(offsetDateTime.toInstant());
  }
}
