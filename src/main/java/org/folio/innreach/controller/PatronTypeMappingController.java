package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.service.PatronTypeMappingService;
import org.folio.innreach.dto.PatronTypeMappingDTO;
import org.folio.innreach.dto.PatronTypeMappingsDTO;
import org.folio.innreach.rest.resource.PatronTypeMappingApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inn-reach/central-servers")
public class PatronTypeMappingController implements PatronTypeMappingApi {

  private final PatronTypeMappingService service;

  @Override
  @GetMapping("/{centralServerId}/patron-type-mappings")
  public ResponseEntity<PatronTypeMappingsDTO> getPatronTypeMappingsByServerId(@PathVariable UUID centralServerId,
                                                                               Integer offset, Integer limit) {

    var mappings = service.getAll(centralServerId, offset, limit);

    return ResponseEntity.ok(mappings);
  }

  @Override
  @GetMapping("/{centralServerId}/patron-type-mappings/{id}")
  public ResponseEntity<PatronTypeMappingDTO> getPatronTypeMappingById(@PathVariable UUID centralServerId,
                                                                       @PathVariable UUID id) {

    var mapping = service.get(centralServerId, id);

    return ResponseEntity.ok(mapping);
  }

  @Override
  @PostMapping("/{centralServerId}/patron-type-mappings")
  public ResponseEntity<PatronTypeMappingDTO> createPatronTypeMapping(@PathVariable UUID centralServerId,
                                                                      PatronTypeMappingDTO patronTypeMappingDTO) {

    var mapping = service.create(centralServerId, patronTypeMappingDTO);

    return ResponseEntity.status(HttpStatus.CREATED).body(mapping);
  }

  @Override
  @PutMapping("/{centralServerId}/patron-type-mappings")
  public ResponseEntity<Void> updatePatronTypeMappings(@PathVariable UUID centralServerId,
                                                       PatronTypeMappingsDTO patronTypeMappingsDTO) {

    service.updateAll(centralServerId, patronTypeMappingsDTO);

    return ResponseEntity.noContent().build();
  }

  @Override
  @PutMapping("/{centralServerId}/patron-type-mappings/{id}")
  public ResponseEntity<Void> updatePatronTypeMapping(@PathVariable UUID centralServerId, @PathVariable UUID id,
                                                      PatronTypeMappingDTO patronTypeMappingDTO) {

    service.update(centralServerId, id, patronTypeMappingDTO);

    return ResponseEntity.noContent().build();
  }

  @Override
  @DeleteMapping("/{centralServerId}/patron-type-mappings/{id}")
  public ResponseEntity<Void> deletePatronTypeMapping(@PathVariable UUID centralServerId, @PathVariable UUID id) {
    service.delete(centralServerId, id);

    return ResponseEntity.noContent().build();
  }
}
