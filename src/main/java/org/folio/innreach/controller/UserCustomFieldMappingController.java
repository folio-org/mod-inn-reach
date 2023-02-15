package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.service.UserCustomFieldMappingService;
import org.folio.innreach.dto.UserCustomFieldMappingDTO;
import org.folio.innreach.rest.resource.UserCustomFieldMappingsApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings")
public class UserCustomFieldMappingController implements UserCustomFieldMappingsApi {

  private final UserCustomFieldMappingService service;

  @Override
  @GetMapping
  public ResponseEntity<UserCustomFieldMappingDTO> getUserCustomFieldMapping(@PathVariable UUID centralServerId) {

    var mappings = service.getMapping(centralServerId);

    return ResponseEntity.ok(mappings);
  }

  @Override
  @PostMapping
  public ResponseEntity<UserCustomFieldMappingDTO> createUserCustomFieldMapping(@PathVariable UUID centralServerId,
                                                                                @Valid UserCustomFieldMappingDTO userCustomFieldMappingDTO) {
    var createdMapping = service.createMapping(centralServerId, userCustomFieldMappingDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdMapping);
  }

  @Override
  @PutMapping
  public ResponseEntity<Void> updateUserCustomFieldMapping(@PathVariable UUID centralServerId,
                                                           @Valid UserCustomFieldMappingDTO userCustomFieldMappingDTO) {

    service.updateMapping(centralServerId, userCustomFieldMappingDTO);

    return ResponseEntity.noContent().build();
  }
}
