package org.folio.innreach.controller.exception;

import static org.folio.innreach.util.ListUtils.mapItems;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.List;

import lombok.extern.log4j.Log4j2;
import org.folio.innreach.external.exception.InnReachGatewayException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.folio.innreach.domain.exception.CirculationException;
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

  @ExceptionHandler({IllegalArgumentException.class, InnReachException.class, CirculationException.class, InnReachGatewayException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Error handleBadRequestException(Exception e) {
    log.error("Unexpected exception: " + e.getMessage(), e);

    return createError(HttpStatus.BAD_REQUEST, e.getMessage());
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

  @ExceptionHandler(DataIntegrityViolationException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public Error handleDataIntegrityViolationException(DataIntegrityViolationException e) {
    return createError(HttpStatus.CONFLICT, e.getMessage());
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
    return mapItems(e.getBindingResult().getFieldErrors(), this::mapFieldErrorToValidationError);
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
}
