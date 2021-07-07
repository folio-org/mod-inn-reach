package org.folio.innreach.external.client.feign.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

public class InnReachTextJsonHttpMessageConverter extends MappingJackson2HttpMessageConverter {

    public InnReachTextJsonHttpMessageConverter() {
      var mediaTypeList = new ArrayList<>(this.getSupportedMediaTypes());
      mediaTypeList.add(new MediaType("text", "json", StandardCharsets.UTF_8));
      this.setSupportedMediaTypes(mediaTypeList);
    }
}
