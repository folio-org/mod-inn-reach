package org.folio.innreach.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.CentralPatronTypeMappingService;
import org.folio.innreach.dto.CentralPatronTypeMappingsDTO;
import org.folio.innreach.rest.resource.CentralPatronTypeMappingsApi;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inn-reach/central-servers/{centralServerId}/central-patron-type-mappings")
public class CentralPatronTypeMappingController implements CentralPatronTypeMappingsApi {

  private final CentralPatronTypeMappingService centralPatronTypeMappingService;

  @Override
  @GetMapping
  public ResponseEntity<CentralPatronTypeMappingsDTO> getCentralPatronTypeMappings(
      @PathVariable("centralServerId") UUID centralServerId, Integer offset, Integer limit) {
    var centralPatronTypeMappings = centralPatronTypeMappingService
      .getCentralPatronTypeMappings(centralServerId, offset, limit);

    return ResponseEntity.ok(centralPatronTypeMappings);
  }

  @Override
  @PutMapping
  public ResponseEntity<Void> updateCentralPatronTypeMappings(@PathVariable("centralServerId") UUID centralServerId,
                                                              CentralPatronTypeMappingsDTO centralPatronTypeMappingsDTO) {
    centralPatronTypeMappingService.updateCentralPatronTypeMappings(centralServerId, centralPatronTypeMappingsDTO);

    return ResponseEntity.noContent().build();
  }
}
