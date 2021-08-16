package org.folio.innreach.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.PatronTypeMappingService;
import org.folio.innreach.dto.PatronTypeMappingsDTO;
import org.folio.innreach.rest.resource.PatronTypeMappingApi;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inn-reach/central-servers")
public class PatronTypeMappingController implements PatronTypeMappingApi {

  private final PatronTypeMappingService service;

  @Override
  @GetMapping("/{centralServerId}/patron-type-mappings")
  public ResponseEntity<PatronTypeMappingsDTO> getPatronTypeMappingsByServerId(@PathVariable UUID centralServerId,
                                                                               Integer offset, Integer limit) {

    var mappings = service.getAllMappings(centralServerId, offset, limit);

    return ResponseEntity.ok(mappings);
  }

  @Override
  @PutMapping("/{centralServerId}/patron-type-mappings")
  public ResponseEntity<Void> updatePatronTypeMappings(@PathVariable UUID centralServerId,
                                                       PatronTypeMappingsDTO patronTypeMappingsDTO) {

    service.updateAllMappings(centralServerId, patronTypeMappingsDTO);

    return ResponseEntity.noContent().build();
  }

}
