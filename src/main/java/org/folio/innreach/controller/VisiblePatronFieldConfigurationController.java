package org.folio.innreach.controller;

import java.util.UUID;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.VisiblePatronFieldConfigurationService;
import org.folio.innreach.dto.VisiblePatronFieldConfigurationDTO;
import org.folio.innreach.rest.resource.VisiblePatronFieldConfigurationApi;

@RequiredArgsConstructor
@RestController
@RequestMapping("/inn-reach/central-servers/{centralServerId}/visible-patron-field-configuration")
public class VisiblePatronFieldConfigurationController implements VisiblePatronFieldConfigurationApi {

  @Autowired
  private final VisiblePatronFieldConfigurationService service;

  @Override
  @GetMapping
  public ResponseEntity<VisiblePatronFieldConfigurationDTO> getConfigurationByCentralServerId(@PathVariable UUID centralServerId) {
    var configuration = service.get(centralServerId);
    return ResponseEntity.ok(configuration);
  }

  @Override
  @PostMapping
  public ResponseEntity<VisiblePatronFieldConfigurationDTO> createConfiguration(@PathVariable UUID centralServerId, @Valid VisiblePatronFieldConfigurationDTO dto) {
    var createdConfiguration = service.create(centralServerId, dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdConfiguration);
  }

  @Override
  @PutMapping
  public ResponseEntity<Void> updateConfiguration(@PathVariable UUID centralServerId, @Valid VisiblePatronFieldConfigurationDTO dto) {
    service.update(centralServerId, dto);
    return ResponseEntity.noContent().build();
  }

}
