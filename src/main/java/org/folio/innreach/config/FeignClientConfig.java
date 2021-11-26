package org.folio.innreach.config;

import feign.Client;
import feign.codec.ErrorDecoder;
import feign.okhttp.OkHttpClient;
import org.springframework.cloud.openfeign.clientconfig.OkHttpFeignConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(OkHttpFeignConfiguration.class)
public class FeignClientConfig {

  @Bean
  public Client feignClient(okhttp3.OkHttpClient okHttpClient) {
    return new OkHttpClient(okHttpClient);
  }

  @Bean("feignDefaultErrorDecoder")
  public ErrorDecoder defaultErrorDecoder() {
    return new ErrorDecoder.Default();
  }

}
