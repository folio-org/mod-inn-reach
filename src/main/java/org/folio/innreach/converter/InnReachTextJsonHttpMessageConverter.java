package org.folio.innreach.converter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class InnReachTextJsonHttpMessageConverter extends JacksonJsonHttpMessageConverter {

  public InnReachTextJsonHttpMessageConverter(JsonMapper objectMapper) {
    super(objectMapper);
    var mediaTypeList = new ArrayList<>(this.getSupportedMediaTypes());
    mediaTypeList.add(new MediaType("text", "json", StandardCharsets.UTF_8));
    this.setSupportedMediaTypes(mediaTypeList);
  }

}
