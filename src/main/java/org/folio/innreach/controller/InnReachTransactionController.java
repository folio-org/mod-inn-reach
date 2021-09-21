package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.dto.TransactionItemHoldDTO;
import org.folio.innreach.rest.resource.InnReachTransactionApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RequiredArgsConstructor
@RestController
@RequestMapping("/innreach/v2/circ/")
public class InnReachTransactionController implements InnReachTransactionApi {

  private final InnReachTransactionService service;

  @Override
  @PostMapping("/itemHold/{trackingId}/{centralCode}")
  public ResponseEntity<Void> createInnReachTransactionItemHold(@PathVariable String trackingId,
                                                                @PathVariable String centralCode,
                                                                @Valid TransactionItemHoldDTO dto) {
    service.createInnReachTransactionItemHold(trackingId, centralCode, dto);
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}
