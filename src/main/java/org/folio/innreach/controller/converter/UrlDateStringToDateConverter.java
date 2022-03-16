package org.folio.innreach.controller.converter;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class UrlDateStringToDateConverter implements Converter<String, Date> {

  @Override
  public Date convert(String source) {
    return Date.from(
        OffsetDateTime.parse(URLDecoder.decode(source, StandardCharsets.UTF_8),
            DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
    );
  }

}