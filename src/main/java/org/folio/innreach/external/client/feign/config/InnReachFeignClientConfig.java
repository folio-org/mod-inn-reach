package org.folio.innreach.external.client.feign.config;

import feign.Client;
import feign.Request;
import feign.codec.ErrorDecoder;
import feign.okhttp.OkHttpClient;
import org.folio.innreach.config.props.InnReachHttpClientProperties;
import org.springframework.context.annotation.Bean;

import org.folio.innreach.external.client.feign.error.InnReachFeignErrorDecoder;
import java.util.concurrent.TimeUnit;

public class InnReachFeignClientConfig {

  @Bean
  public Client feignClient(okhttp3.OkHttpClient okHttpClient) {
    return new OkHttpClient(okHttpClient);
  }

  @Bean
  public ErrorDecoder innReachErrorDecoder() {
    return new InnReachFeignErrorDecoder();
  }

  @Bean
  public Request.Options requestOptions(InnReachHttpClientProperties props) {
    return new Request.Options(
      props.connectTimeoutMs(), TimeUnit.MILLISECONDS,
      props.readTimeoutMs(), TimeUnit.MILLISECONDS,
      true);
  }

}
