package org.folio.innreach.client;

import java.util.Optional;
import java.util.UUID;

import feign.codec.ErrorDecoder;
import feign.error.AnnotationErrorDecoder;
import feign.error.ErrorCodes;
import feign.error.ErrorHandling;
import org.apache.http.HttpStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.domain.exception.ResourceVersionConflictException;
import org.folio.innreach.dto.Holding;

@FeignClient(name = "holdings-storage", configuration = HoldingsStorageClient.Config.class, decode404 = true)
public interface HoldingsStorageClient {

  @GetMapping("/holdings/{holdingId}")
  Optional<Holding> findHolding(@PathVariable("holdingId") UUID holdingId);

  @PostMapping("/holdings")
  Holding createHolding(@RequestBody Holding holding);

  @ErrorHandling(codeSpecific = {
      @ErrorCodes(codes = {HttpStatus.SC_CONFLICT}, generate = ResourceVersionConflictException.class)
  })
  @PutMapping("/holdings/{holdingId}")
  Holding updateHolding(@PathVariable UUID holdingId, @RequestBody Holding holding);

  class Config extends FolioFeignClientConfig {

    @Bean
    public ErrorDecoder holdingsStorageClientErrorDecoder() {
      return AnnotationErrorDecoder.builderFor(HoldingsStorageClient.class).build();
    }

  }

}
