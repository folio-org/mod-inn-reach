package org.folio.innreach.external.client.feign.config;

import feign.RequestInterceptor;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.annotation.Bean;

public class FolioFeignClientConfig {

  @Bean
  public RequestInterceptor requestInterceptor(FolioExecutionContext folioExecutionContext) {
    return new FolioRequestInterceptor(folioExecutionContext);
  }

}
