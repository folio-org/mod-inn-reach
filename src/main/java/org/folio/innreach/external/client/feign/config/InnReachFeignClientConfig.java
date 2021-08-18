package org.folio.innreach.external.client.feign.config;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

import org.folio.innreach.external.client.feign.error.FeignErrorDecoder;

public class InnReachFeignClientConfig {

  @Bean
  public ErrorDecoder feignErrorDecoder() {
    return new FeignErrorDecoder();
  }

}
