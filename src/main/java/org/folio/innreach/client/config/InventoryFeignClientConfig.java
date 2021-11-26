package org.folio.innreach.client.config;

import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;

import org.folio.innreach.client.customization.InventoryErrorDecoder;
import org.folio.innreach.util.JsonHelper;

public class InventoryFeignClientConfig extends FolioFeignClientConfig {

  @Bean
  public ErrorDecoder inventoryErrorDecoder(
      @Qualifier("feignDefaultErrorDecoder") ErrorDecoder defaultErrorDecoder, JsonHelper jsonHelper) {
    return new InventoryErrorDecoder(defaultErrorDecoder, jsonHelper);
  }

}