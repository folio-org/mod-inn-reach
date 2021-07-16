package org.folio.innreach.controller;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.exception.UniqueConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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

@Log4j2
@RestControllerAdvice
public class ExceptionHandlerController {

  @ExceptionHandler(EntityNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Error handleEntityNotFoundException(EntityNotFoundException e) {
    return createError(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ValidationErrorsDTO handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    var validationErrorsDTO = createValidationErrors();
    validationErrorsDTO.setValidationErrors(collectValidationErrors(e));

    return validationErrorsDTO;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ValidationErrorsDTO handleConstraintValidationException(ConstraintViolationException e) {
    var errors = createValidationErrors();

    for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
      var fieldError = new ValidationErrorDTO();
      fieldError.setFieldName(violation.getPropertyPath().toString());
      fieldError.setMessage(violation.getMessage());

      errors.addValidationErrorsItem(fieldError);
    }

    return errors;
  }

  @ExceptionHandler(InnReachException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Error handleInnReachException(InnReachException e) {
    return createError(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Error handleDataIntegrityViolationException(DataIntegrityViolationException e) {
    return createError(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Error handleException(Exception e) {
    log.error("Unexpected exception: " + e.getMessage(), e);

    return createError(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
  }

  private ValidationErrorsDTO createValidationErrors() {
    var errors = new ValidationErrorsDTO();
    errors.setCode(HttpStatus.BAD_REQUEST.value());
    errors.setMessage("Validation failed");
    return errors;
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

  @ExceptionHandler(AuthenticationException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public Error handleAuthenticationException(AuthenticationException e) {
    return createError(HttpStatus.UNAUTHORIZED, e.getMessage());
  }

  private Error createError(HttpStatus code, String message) {
    var error = new Error();
    error.setCode(Integer.toString(code.value()));
    error.setMessage(message);
    return error;
  }

  @ExceptionHandler(UniqueConstraintViolationException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public Error handleUniqueConstraintViolationException(UniqueConstraintViolationException e) {
    return createError(HttpStatus.CONFLICT, e.getMessage());
  }
}
