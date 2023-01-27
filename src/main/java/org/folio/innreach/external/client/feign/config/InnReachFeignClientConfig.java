package org.folio.innreach.external.client.feign.config;

import feign.Client;
import feign.codec.ErrorDecoder;
import feign.okhttp.OkHttpClient;
import org.springframework.context.annotation.Bean;

import org.folio.innreach.external.client.feign.error.InnReachFeignErrorDecoder;

public class InnReachFeignClientConfig {

  @Bean
  public Client feignClient(okhttp3.OkHttpClient okHttpClient) {
    return new OkHttpClient(okHttpClient);
  }

  @Bean
  public ErrorDecoder innReachErrorDecoder() {
    return new InnReachFeignErrorDecoder();
  }

}
