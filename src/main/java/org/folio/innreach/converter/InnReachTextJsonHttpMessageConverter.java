package org.folio.innreach.converter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

@Component
public class InnReachTextJsonHttpMessageConverter extends MappingJackson2HttpMessageConverter {

  public InnReachTextJsonHttpMessageConverter(ObjectMapper objectMapper) {
    super(objectMapper);
    var mediaTypeList = new ArrayList<>(this.getSupportedMediaTypes());
    mediaTypeList.add(new MediaType("text", "json", StandardCharsets.UTF_8));
    this.setSupportedMediaTypes(mediaTypeList);
  }

}
