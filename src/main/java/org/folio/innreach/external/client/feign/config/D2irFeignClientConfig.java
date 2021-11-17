package org.folio.innreach.external.client.feign.config;

import feign.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.clientconfig.OkHttpFeignConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.external.service.InnReachAuthExternalService;

@Import(OkHttpFeignConfiguration.class)
public class D2irFeignClientConfig {

  @Bean
  public Client enrichUrlAndHeadersClient(@Autowired InnReachAuthExternalService authService,
                                          @Autowired CentralServerService centralServerService,
                                          @Autowired okhttp3.OkHttpClient okHttpClient) {
    return new D2irFeignClient(authService, centralServerService, okHttpClient);
  }

}
