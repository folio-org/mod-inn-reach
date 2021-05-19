package org.folio.innreach.controller;

import org.folio.innreach.domain.dto.ErrorResponseDTO;
import org.folio.innreach.domain.dto.ValidationErrorResponseDTO;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.external.exception.InnReachException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Date;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ExceptionHandlerController {

  @ExceptionHandler(EntityNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponseDTO handleEntityNotFoundException(EntityNotFoundException e) {
    return new ErrorResponseDTO(new Date(), HttpStatus.NOT_FOUND.value(), e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ValidationErrorResponseDTO handleValidationException(MethodArgumentNotValidException e) {
    var validationErrorMessages = e.getBindingResult().getAllErrors()
      .stream()
      .map(ObjectError::getDefaultMessage)
      .collect(Collectors.toSet());

    return new ValidationErrorResponseDTO(new Date(), HttpStatus.BAD_REQUEST.value(),
      "Validation failed", validationErrorMessages);
  }

  @ExceptionHandler(InnReachException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponseDTO handleInnReachException(InnReachException e) {
    return new ErrorResponseDTO(new Date(), HttpStatus.BAD_REQUEST.value(), e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorResponseDTO handleException(Exception e) {
    return new ErrorResponseDTO(new Date(), HttpStatus.BAD_REQUEST.value(), e.getMessage());
  }
}
