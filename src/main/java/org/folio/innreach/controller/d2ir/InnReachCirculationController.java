package org.folio.innreach.controller.d2ir;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.CirculationService;
import org.folio.innreach.dto.CirculationRequestDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.ItemShippedDTO;
import org.folio.innreach.dto.PatronHoldDTO;
import org.folio.innreach.dto.TransferRequestDTO;
import org.folio.innreach.rest.resource.InnReachCirculationApi;

@RestController
@Validated
@RequestMapping("/inn-reach/d2ir/circ")
@RequiredArgsConstructor
public class InnReachCirculationController implements InnReachCirculationApi {

  private final CirculationService circulationService;

  @Override
  @RequestMapping(value = "/{circulationOperationName}/{trackingId}/{centralCode}", method = {RequestMethod.POST, RequestMethod.PUT})
  public ResponseEntity<InnReachResponseDTO> processCirculationRequest(@PathVariable String trackingId,
                                                                       @PathVariable String centralCode,
                                                                       @PathVariable String circulationOperationName,
                                                                       CirculationRequestDTO circulationRequest) {
    var innReachResponse = circulationService.processCirculationRequest(trackingId, centralCode, circulationOperationName, circulationRequest);
    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PostMapping("/patronHold/{trackingId}/{centralCode}")
  public ResponseEntity<InnReachResponseDTO> patronHold(@PathVariable String trackingId,
      @PathVariable String centralCode, PatronHoldDTO patronHold) {
    var innReachResponse = circulationService.initiatePatronHold(trackingId, centralCode, patronHold);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping("/itemshipped/{trackingId}/{centralCode}")
  public ResponseEntity<InnReachResponseDTO> itemShipped(@PathVariable String trackingId,
      @PathVariable String centralCode, ItemShippedDTO itemShipped) {
    var innReachResponse = circulationService.trackShippedItem(trackingId, centralCode, itemShipped);

    return ResponseEntity.ok(innReachResponse);
  }

  @Override
  @PutMapping("/transferrequest/{trackingId}/{centralCode}")
  public ResponseEntity<InnReachResponseDTO> transferRequest(@PathVariable String trackingId,
      @PathVariable String centralCode, TransferRequestDTO transferRequest) {
    var innReachResponse = circulationService.transferItem(trackingId, centralCode, transferRequest);

    return ResponseEntity.ok(innReachResponse);
  }

}
