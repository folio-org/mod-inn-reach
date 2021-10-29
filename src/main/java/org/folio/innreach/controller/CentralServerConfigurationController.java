package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.CentralServerConfigurationService;
import org.folio.innreach.dto.CentralServerAgenciesDTO;
import org.folio.innreach.dto.CentralServerItemTypesDTO;
import org.folio.innreach.rest.resource.CentralServerConfigurationApi;

@Log4j2
@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/inn-reach/central-servers/")
public class CentralServerConfigurationController implements CentralServerConfigurationApi {

  private final CentralServerConfigurationService service;

  @Override
  @GetMapping("/agencies")
  public ResponseEntity<CentralServerAgenciesDTO> getCentralServerAgencies() {
    var agencies = service.getAllAgencies();

    return ResponseEntity.ok(agencies);
  }

  @Override
  @GetMapping("/item-types")
  public ResponseEntity<CentralServerItemTypesDTO> getCentralServerItemTypes() {
    var itemTypes = service.getAllItemTypes();

    return ResponseEntity.ok(itemTypes);
  }

}
