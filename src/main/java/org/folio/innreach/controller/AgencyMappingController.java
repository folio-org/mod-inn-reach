package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.service.AgencyMappingService;
import org.folio.innreach.dto.AgencyLocationMappingDTO;
import org.folio.innreach.rest.resource.AgencyMappingsApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/inn-reach/central-servers")
public class AgencyMappingController implements AgencyMappingsApi {

  @Autowired
  private AgencyMappingService service;

  @Override
  @GetMapping("/{centralServerId}/agency-mappings")
  public ResponseEntity<AgencyLocationMappingDTO> getAgencyMappingsByServerId(@PathVariable UUID centralServerId) {
    var mapping = service.getMapping(centralServerId);

    return ResponseEntity.ok(mapping);
  }

  @Override
  @PutMapping("/{centralServerId}/agency-mappings")
  public ResponseEntity<Void> putAgencyMappings(@PathVariable UUID centralServerId,
                                                AgencyLocationMappingDTO mapping) {

    service.updateMapping(centralServerId, mapping);

    return ResponseEntity.noContent().build();
  }

}
