package org.folio.innreach.controller.exception;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.folio.innreach.domain.exception.CirculationException;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.mapper.InnReachErrorMapper;

@Order(HIGHEST_PRECEDENCE)
@Log4j2
@RequiredArgsConstructor
@RestControllerAdvice(basePackages = "org.folio.innreach.controller.d2ir")
public class D2irExceptionHandlerController {

  private final InnReachErrorMapper mapper;

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public InnReachResponseDTO handleException(Exception e) {
    log.error("Unexpected exception: " + e.getMessage(), e);

    return failed(e);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public InnReachResponseDTO handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    log.warn("Argument validation failed", e);

    var bindingResult = e.getBindingResult();
    var innReachErrors = bindingResult.getFieldErrors().stream()
      .map(mapper::toInnReachError)
      .collect(Collectors.toList());

    return failed("Argument validation failed")
      .errors(innReachErrors);
  }

  @ExceptionHandler({EntityNotFoundException.class, IllegalArgumentException.class, CirculationException.class,
    MissingRequestHeaderException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public InnReachResponseDTO handleEntityNotFoundException(Exception e) {
    log.warn(e.getMessage(), e);

    return failed(e);
  }

  private InnReachResponseDTO failed(Exception e) {
    return failed(e.getMessage());
  }

  private InnReachResponseDTO failed(String reason) {
    return new InnReachResponseDTO()
        .status("failed")
        .reason(reason);
  }

}
