package org.folio.innreach.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.InnReachTransactionActionService;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.dto.CancelTransactionHoldDTO;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.InnReachTransactionFilterParametersDTO;
import org.folio.innreach.dto.InnReachTransactionsDTO;
import org.folio.innreach.dto.PatronHoldCheckInResponseDTO;
import org.folio.innreach.dto.TransactionCheckOutResponseDTO;
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
  @PostMapping("/{id}/patronhold/return-item/{servicePointId}")
  public ResponseEntity<Void> returnPatronHoldItem(@PathVariable UUID id, @PathVariable UUID servicePointId) {
    transactionActionService.returnPatronHoldItem(id, servicePointId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PostMapping("/{id}/itemhold/transfer-item/{itemBarcode}")
  public ResponseEntity<Void> transferItemHold(@PathVariable UUID id, @PathVariable String itemBarcode) {
    transactionActionService.transferItemHold(id, itemBarcode);
    return ResponseEntity.noContent().build();
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
  public ResponseEntity<TransactionCheckOutResponseDTO> checkOutItemHoldItem(@PathVariable String itemBarcode,
                                                                             @PathVariable UUID servicePointId) {
    var response = transactionActionService.checkOutItemHoldItem(itemBarcode, servicePointId);
    return ResponseEntity.ok(response);
  }

  @Override
  @PostMapping("/{id}/patronhold/check-out-item/{servicePointId}")
  public ResponseEntity<TransactionCheckOutResponseDTO> checkOutPatronHoldItem(@PathVariable UUID id,
                                                                               @PathVariable UUID servicePointId) {
    var response = transactionActionService.checkOutPatronHoldItem(id, servicePointId);
    return ResponseEntity.ok(response);
  }

  @Override
  @PostMapping("/{id}/localhold/check-out-item/{servicePointId}")
  public ResponseEntity<TransactionCheckOutResponseDTO> checkOutLocalHoldItem(@PathVariable UUID id,
                                                                              @PathVariable UUID servicePointId) {
    var response = transactionActionService.checkOutLocalHoldItem(id, servicePointId);
    return ResponseEntity.ok(response);
  }

  @Override
  @PostMapping("/{id}/patronhold/cancel")
  public ResponseEntity<InnReachTransactionDTO> cancelPatronHoldTransaction(@PathVariable UUID id,
      CancelTransactionHoldDTO cancelRequest) {

    var response = transactionActionService.cancelPatronHold(id, cancelRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  @PostMapping("/{id}/itemhold/cancel")
  public ResponseEntity<Void> cancelItemHoldTransaction(@PathVariable UUID id,
      CancelTransactionHoldDTO cancelRequest) {

    transactionActionService.cancelItemHold(id, cancelRequest);
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping
  public ResponseEntity<InnReachTransactionsDTO> getAllTransactions(Integer offset,
                                                                    Integer limit,
                                                                    InnReachTransactionFilterParametersDTO parameters) {
    var transactions = transactionService.getAllTransactions(offset, limit, parameters);
    return ResponseEntity.ok(transactions);
  }

  @Override
  @PutMapping("/{id}")
  public ResponseEntity<Void> updateInnReachTransaction(@PathVariable UUID id, InnReachTransactionDTO transaction) {
    transactionService.updateInnReachTransaction(id, transaction);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PostMapping("/{id}/itemhold/finalcheckin/{servicePointId}")
  public ResponseEntity<Void> finalCheckInItemHold(@PathVariable UUID id, @PathVariable UUID servicePointId) {
    transactionActionService.finalCheckInItemHold(id, servicePointId);
    return ResponseEntity.noContent().build();
  }
}
