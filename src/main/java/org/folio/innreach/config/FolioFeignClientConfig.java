package org.folio.innreach.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

import org.folio.spring.FolioExecutionContext;

public class FolioFeignClientConfig {

  @Bean
  public RequestInterceptor requestInterceptor(FolioExecutionContext folioExecutionContext) {
    return new FolioRequestInterceptor(folioExecutionContext);
  }

  @Bean
  public ErrorDecoder defaultErrorDecoder() {
    return new ErrorDecoder.Default();
  }

}
