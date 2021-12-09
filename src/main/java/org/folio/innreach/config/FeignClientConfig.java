package org.folio.innreach.config;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

  @Bean("feignDefaultErrorDecoder")
  public ErrorDecoder defaultErrorDecoder() {
    return new ErrorDecoder.Default();
  }

}
