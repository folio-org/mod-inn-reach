package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.dto.D2irResponseDTO;
import org.folio.innreach.dto.TransactionItemHoldDTO;
import org.folio.innreach.rest.resource.InnReachTransactionApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping("/innreach/v2/circ/")
public class InnReachTransactionController implements InnReachTransactionApi {

  private final InnReachTransactionService service;

  @Override
  @PostMapping("/itemHold/{trackingId}/{centralCode}")
  public ResponseEntity<D2irResponseDTO> createInnReachTransactionItemHold(@PathVariable String trackingId,
                                                                           @PathVariable String centralCode,
                                                                           @Valid TransactionItemHoldDTO dto) {
    var response = service.createInnReachTransactionItemHold(trackingId, centralCode, dto);
    var status = response.getStatus().equals("ok") ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
    return new ResponseEntity<>(response, status);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public D2irResponseDTO handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    log.warn(e.getMessage());
    var response = new D2irResponseDTO();
    response.setStatus("failed");
    response.setReason(e.getMessage());
    return response;
  }
}
