package org.folio.innreach.controller.converter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToDateConverter implements Converter<String, Date> {

  @Override
  public Date convert(String source) {
    return Date.from(
        OffsetDateTime.parse(source, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
    );
  }

}