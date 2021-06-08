package org.folio.innreach.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.dto.Error;
import org.folio.innreach.dto.ValidationErrorDTO;
import org.folio.innreach.dto.ValidationErrorsDTO;
import org.folio.innreach.external.exception.InnReachException;

@RestControllerAdvice
public class ExceptionHandlerController {

  @ExceptionHandler(EntityNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Error handleEntityNotFoundException(EntityNotFoundException e) {
    return createError(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ValidationErrorsDTO handleValidationException(MethodArgumentNotValidException e) {
    var validationErrorsDTO = new ValidationErrorsDTO();
    validationErrorsDTO.setCode(HttpStatus.BAD_REQUEST.value());
    validationErrorsDTO.setMessage("Validation failed");
    validationErrorsDTO.setValidationErrors(collectValidationErrors(e));

    return validationErrorsDTO;
  }

  private List<ValidationErrorDTO> collectValidationErrors(MethodArgumentNotValidException e) {
    return e.getBindingResult()
	    .getFieldErrors()
      .stream()
      .map(this::mapFieldErrorToValidationError)
      .collect(Collectors.toList());
  }

  private ValidationErrorDTO mapFieldErrorToValidationError(FieldError fieldError) {
    var validationErrorDTO = new ValidationErrorDTO();
    validationErrorDTO.setFieldName(fieldError.getField());
    validationErrorDTO.setMessage(fieldError.getDefaultMessage());
    return validationErrorDTO;
  }

  @ExceptionHandler(InnReachException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Error handleInnReachException(InnReachException e) {
    return createError(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(AuthenticationException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public Error handleAuthenticationException(AuthenticationException e) {
    return createError(HttpStatus.UNAUTHORIZED, e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Error handleException(Exception e) {
    return createError(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
  }

  private Error createError(HttpStatus code, String message) {
    var error = new Error();
    error.setCode(Integer.toString(code.value()));
    error.setMessage(message);
    return error;
  }
}
