package org.folio.innreach.controller.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.folio.innreach.dto.InnReachResponseDTO;

@Log4j2
@RestControllerAdvice(basePackages = "org.folio.innreach.controller.d2ir")
public class D2irExceptionHandlerController {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public InnReachResponseDTO handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    log.warn(e.getMessage());
    var response = new InnReachResponseDTO();
    response.setStatus("failed");
    response.setReason(e.getMessage());
    return response;
  }

}
