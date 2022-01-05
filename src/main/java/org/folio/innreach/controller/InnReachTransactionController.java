package org.folio.innreach.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.InnReachTransactionActionService;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.InnReachTransactionFilterParametersDTO;
import org.folio.innreach.dto.InnReachTransactionsDTO;
import org.folio.innreach.dto.ItemHoldCheckOutResponseDTO;
import org.folio.innreach.dto.PatronHoldCheckInResponseDTO;
import org.folio.innreach.rest.resource.InnReachTransactionApi;

@Log4j2
@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/inn-reach/transactions")
public class InnReachTransactionController implements InnReachTransactionApi {

  private final InnReachTransactionActionService transactionActionService;
  private final InnReachTransactionService transactionService;

  @Override
  @GetMapping("/{id}")
  public ResponseEntity<InnReachTransactionDTO> getInnReachTransaction(@PathVariable UUID id) {
    var innReachTransaction = transactionService.getInnReachTransaction(id);
    return ResponseEntity.ok(innReachTransaction);
  }

  @Override
  @PostMapping("/{id}/receive-item/{servicePointId}")
  public ResponseEntity<PatronHoldCheckInResponseDTO> checkInPatronHoldItem(@PathVariable UUID id,
                                                                            @PathVariable UUID servicePointId) {
    var response = transactionActionService.checkInPatronHoldItem(id, servicePointId);
    return ResponseEntity.ok(response);
  }

  @Override
  @PostMapping("/{id}/receive-unshipped-item/{servicePointId}/{itemBarcode}")
  public ResponseEntity<PatronHoldCheckInResponseDTO> checkInPatronHoldUnshippedItem(@PathVariable UUID id,
                                                                                     @PathVariable UUID servicePointId,
                                                                                     @PathVariable String itemBarcode) {
    var response = transactionActionService.checkInPatronHoldUnshippedItem(id, servicePointId, itemBarcode);
    return ResponseEntity.ok(response);
  }

  @Override
  @PostMapping("/{itemBarcode}/check-out-item/{servicePointId}")
  public ResponseEntity<ItemHoldCheckOutResponseDTO> checkOutItemHoldItem(@PathVariable String itemBarcode,
                                                                          @PathVariable UUID servicePointId) {
    var response = transactionActionService.checkOutItemHoldItem(itemBarcode, servicePointId);
    return ResponseEntity.ok(response);
  }

  @Override
  @GetMapping
  public ResponseEntity<InnReachTransactionsDTO> getAllTransactions(Integer offset,
                                                                    Integer limit,
                                                                    InnReachTransactionFilterParametersDTO parameters) {
    var transactions = transactionService.getAllTransactions(offset, limit, parameters);
    return ResponseEntity.ok(transactions);
  }
}
