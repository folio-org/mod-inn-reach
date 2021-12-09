package org.folio.innreach.external.client.feign.config;

import feign.Client;
import feign.okhttp.OkHttpClient;
import org.springframework.cloud.openfeign.clientconfig.OkHttpFeignConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import(OkHttpFeignConfiguration.class)
public class InnReachFeignContributionClientConfig {

  @Bean
  public Client feignClient(okhttp3.OkHttpClient okHttpClient) {
    return new OkHttpClient(okHttpClient);
  }

}