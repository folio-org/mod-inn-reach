package org.folio.innreach.controller.exception;

import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.mapper.InnReachErrorMapper;

@Log4j2
@RequiredArgsConstructor
@RestControllerAdvice(basePackages = "org.folio.innreach.controller.d2ir")
public class D2irExceptionHandlerController {

  private final InnReachErrorMapper mapper;

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public InnReachResponseDTO handleException(Exception e) {
    log.warn("Handling exception", e);

    return new InnReachResponseDTO()
      .status("failed")
      .reason(e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public InnReachResponseDTO handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    log.warn("Argument validation failed", e);

    var bindingResult = e.getBindingResult();
    var innReachErrors = bindingResult.getFieldErrors().stream()
      .map(mapper::toInnReachError)
      .collect(Collectors.toList());

    return new InnReachResponseDTO()
      .status("failed")
      .reason("Argument validation failed")
      .errors(innReachErrors);
  }

}
