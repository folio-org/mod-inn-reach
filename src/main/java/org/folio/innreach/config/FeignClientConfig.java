package org.folio.innreach.config;

import feign.codec.ErrorDecoder;
import org.folio.innreach.config.props.InnReachHttpClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(InnReachHttpClientProperties.class)
public class FeignClientConfig {

  @Bean("feignDefaultErrorDecoder")
  public ErrorDecoder defaultErrorDecoder() {
    return new ErrorDecoder.Default();
  }

}
