package org.folio.innreach.external.client.feign;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import static org.folio.innreach.external.InnReachHeaders.X_TO_CODE;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.external.client.feign.config.D2irFeignClientConfig;

@FeignClient(value = "innreach/v2/circ", configuration = D2irFeignClientConfig.class)
public interface InnReachCirculationClient {

  @PostMapping(value = "/{operation}/{trackingId}/{centralCode}",
    consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  InnReachResponseDTO postCircRequest(@RequestHeader(X_TO_CODE) String xToCode,
                                      @PathVariable("operation") String operation,
                                      @PathVariable("trackingId") String trackingId,
                                      @PathVariable("centralCode") String centralCode,
                                      @RequestBody Object payload);

  @PostMapping(value = "/{operation}/{trackingId}/{centralCode}",
    consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  InnReachResponseDTO postCircRequest(@RequestHeader(X_TO_CODE) String xToCode,
                                      @PathVariable("operation") String operation,
                                      @PathVariable("trackingId") String trackingId,
                                      @PathVariable("centralCode") String centralCode);

}
