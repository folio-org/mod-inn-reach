package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.TransactionItemHoldDTO;
import org.folio.innreach.rest.resource.InnReachTransactionApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Log4j2
@RequiredArgsConstructor
@RestController
public class InnReachTransactionController implements InnReachTransactionApi {

  private final RequestService requestService;
  private final InnReachTransactionService transactionService;

  @Override
  @PostMapping("/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}")
  public ResponseEntity<InnReachResponseDTO> createInnReachTransactionItemHold(@PathVariable String trackingId,
                                                                               @PathVariable String centralCode,
                                                                               @Valid TransactionItemHoldDTO dto) {
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

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public InnReachResponseDTO handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    log.warn("Argument validation failed.", e);
    var response = new InnReachResponseDTO();
    response.setStatus("failed");
    response.setReason(e.getMessage());
    return response;
  }
}
