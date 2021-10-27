package org.folio.innreach.controller.d2ir;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.CirculationService;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.rest.resource.InnReachCirculationApi;

@RestController
@RequestMapping("/inn-reach/d2ir/circ")
@RequiredArgsConstructor
public class InnReachCirculationController implements InnReachCirculationApi {

  private final CirculationService circulationService;

  @Override
  @PostMapping("/{circulationOperationName}/{trackingId}/{centralCode}")
  public ResponseEntity<InnReachResponseDTO> processCirculationRequest(@PathVariable String trackingId,
                                                                       @PathVariable String centralCode,
                                                                       @PathVariable String circulationOperationName,
                                                                       @RequestBody @Valid TransactionHoldDTO transactionHold) {
    var innReachResponse = circulationService.processCirculationRequest(trackingId, centralCode, circulationOperationName, transactionHold);
    return ResponseEntity.ok(innReachResponse);
  }
}
