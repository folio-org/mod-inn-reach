package org.folio.innreach.external.client.feign.config;

import static feign.Logger.Level.FULL;
import static feign.Logger.Level.NONE;

import feign.Client;
import feign.Logger;
import feign.codec.ErrorDecoder;
import feign.okhttp.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.clientconfig.OkHttpFeignConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import org.folio.innreach.external.client.feign.error.InnReachFeignErrorDecoder;

@Import(OkHttpFeignConfiguration.class)
public class InnReachFeignClientConfig {

  @Value("${http-logging.enabled:false}")
  private boolean enableLogging;

  @Bean
  public Client feignClient(okhttp3.OkHttpClient okHttpClient) {
    return new OkHttpClient(okHttpClient);
  }

  @Bean
  public ErrorDecoder innReachErrorDecoder() {
    return new InnReachFeignErrorDecoder();
  }

  @Bean
  public Logger.Level feignLoggerLevel() {
    return enableLogging ? FULL : NONE;
  }

}
