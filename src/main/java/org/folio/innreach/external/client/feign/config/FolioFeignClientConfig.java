package org.folio.innreach.external.client.feign.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

import org.folio.spring.FolioExecutionContext;

public class FolioFeignClientConfig {

  @Bean
  public RequestInterceptor requestInterceptor(FolioExecutionContext folioExecutionContext) {
    return new FolioRequestInterceptor(folioExecutionContext);
  }

}
