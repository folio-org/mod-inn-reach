package org.folio.innreach.client;

import java.util.Optional;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.circulation.MoveRequestDTO;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.dto.CheckInRequestDTO;
import org.folio.innreach.dto.CheckInResponseDTO;

@FeignClient(name = "circulation", configuration = FolioFeignClientConfig.class, decode404 = true)
public interface CirculationClient {

  @GetMapping("/requests?query=(itemId=={itemId})")
  ResultList<RequestDTO> queryRequestsByItemId(@PathVariable("itemId") UUID itemId);

  @GetMapping("/requests/{requestId}")
  Optional<RequestDTO> findRequest(@PathVariable("requestId") UUID requestId);

  @PostMapping("/requests")
  RequestDTO sendRequest(@RequestBody RequestDTO requestDTO);

  @PutMapping("/requests/{requestId}")
  RequestDTO updateRequest(@PathVariable("requestId") UUID requestId, @RequestBody RequestDTO request);

  @PostMapping("/requests/{requestId}/move")
  RequestDTO moveRequest(@PathVariable("requestId") UUID requestId, @RequestBody MoveRequestDTO payload);

  @PostMapping("/check-in-by-barcode")
  CheckInResponseDTO checkInByBarcode(CheckInRequestDTO checkIn);

}
