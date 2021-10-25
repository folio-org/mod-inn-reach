package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.AgencyService;
import org.folio.innreach.dto.CentralServerAgenciesDTO;
import org.folio.innreach.rest.resource.CentralServerAgenciesApi;

@Log4j2
@RequiredArgsConstructor
@RestController
@Validated
public class CentralServerAgencyController implements CentralServerAgenciesApi {

  private final AgencyService agencyService;

  @Override
  @GetMapping("/inn-reach/central-servers/agencies")
  public ResponseEntity<CentralServerAgenciesDTO> getCentralServerAgencies() {
    var agencies = agencyService.getAllAgencies();

    return ResponseEntity.ok(agencies);
  }

}