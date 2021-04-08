package org.folio.innreach.controller;

import lombok.extern.log4j.Log4j2;
import org.folio.innreach.dto.Errors;
import org.folio.innreach.dto.ModuleConfiguration;
import org.folio.innreach.dto.ModuleConfigurations;
import org.folio.innreach.rest.resource.ConfigurationsApi;
import org.folio.innreach.service.ConfigurationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import static java.util.Objects.isNull;
import static org.folio.innreach.error.ErrorUtil.CONFIGURATION_NOT_FOUND;
import static org.folio.innreach.error.ErrorUtil.buildErrors;

@Log4j2
@RestController
@RequestMapping(value = "/inn-reach/")
public class ConfigurationsController implements ConfigurationsApi {

  private final ConfigurationsService configurationsService;

  @Autowired
  public ConfigurationsController(ConfigurationsService configurationsService) {
    this.configurationsService = configurationsService;
  }

  @Override
  public ResponseEntity<String> deleteConfigurationById(String configId) {
    configurationsService.deleteConfigurationById(configId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ModuleConfiguration> getConfigurationById(String configId) {
    var configuration = configurationsService.getConfigurationById(configId);
    return isNull(configuration) ? ResponseEntity.notFound().build() : new ResponseEntity<>(configuration, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ModuleConfigurations> getConfigurations(@Min(0) @Max(2147483647) @Valid Integer offset,
    @Min(0) @Max(2147483647) @Valid Integer limit, @Valid String query) {
    var configurations = configurationsService.getConfigurations(offset, limit);
    return new ResponseEntity<>(configurations, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ModuleConfiguration> postConfiguration(@Valid ModuleConfiguration moduleConfiguration) {
    var configuration = configurationsService.postConfiguration(moduleConfiguration);
    return new ResponseEntity<>(configuration, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ModuleConfiguration> putConfiguration(@Valid ModuleConfiguration moduleConfiguration) {
    var configuration = configurationsService.createOrUpdateConfiguration(moduleConfiguration);
    return new ResponseEntity<>(configuration, HttpStatus.OK);
  }

  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  @ExceptionHandler({ MethodArgumentNotValidException.class, DataIntegrityViolationException.class })
  public Errors handleValidationExceptions(Throwable throwable) {
    return buildErrors(throwable);
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler({ EmptyResultDataAccessException.class, EntityNotFoundException.class })
  public String handleNotFoundExceptions() {
    return CONFIGURATION_NOT_FOUND;
  }
}
