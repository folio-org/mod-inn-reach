package org.folio.innreach.controller;

import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.mapper.InnReachErrorMapper;
import org.folio.innreach.rest.resource.InnReachTransactionApi;

@Log4j2
@RequiredArgsConstructor
@RestController
public class InnReachTransactionController implements InnReachTransactionApi {

  private final RequestService requestService;
  private final InnReachTransactionService transactionService;
  private final InnReachErrorMapper mapper;

  @Override
  @PostMapping("/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}")
  public ResponseEntity<InnReachResponseDTO> createInnReachTransactionItemHold(@PathVariable String trackingId,
                                                                               @PathVariable String centralCode,
                                                                               @Valid TransactionHoldDTO dto) {
    var response = transactionService.createInnReachTransactionItemHold(trackingId, centralCode, dto);
    HttpStatus status;
    if (response.getStatus().equals("ok")) {
      status = HttpStatus.OK;
      requestService.createItemRequest(trackingId);
    } else {
      status = HttpStatus.BAD_REQUEST;
    }
    return new ResponseEntity<>(response, status);
  }

  @Override
  @GetMapping("/inn-reach/transactions/{transactionId}")
  public ResponseEntity<InnReachTransactionDTO> getInnReachTransaction(@PathVariable UUID transactionId) {
    var innReachTransaction = transactionService.getInnReachTransaction(transactionId);
    return ResponseEntity.ok(innReachTransaction);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public InnReachResponseDTO handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    log.warn("Argument validation failed.", e);
    var bindingResult = e.getBindingResult();
    var innReachErrors = bindingResult.getFieldErrors().stream().map(mapper::toInnReachError).collect(Collectors.toList());
    var response = new InnReachResponseDTO();
    response.setStatus("failed");
    response.setReason("Argument validation failed.");
    response.setErrors(innReachErrors);
    return response;
  }
}
