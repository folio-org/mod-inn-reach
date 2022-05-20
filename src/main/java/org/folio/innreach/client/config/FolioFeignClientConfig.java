package org.folio.innreach.client.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

import org.folio.innreach.client.customization.FolioRequestInterceptor;
import org.folio.spring.FolioExecutionContext;

public class FolioFeignClientConfig {

  @Bean
  public RequestInterceptor requestInterceptor(FolioExecutionContext folioExecutionContext) {
    return new FolioRequestInterceptor(folioExecutionContext);
  }

}
