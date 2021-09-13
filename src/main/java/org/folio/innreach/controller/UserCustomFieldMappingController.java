package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.service.UserCustomFieldMappingService;
import org.folio.innreach.dto.UserCustomFieldMappingsDTO;
import org.folio.innreach.rest.resource.UserCustomFieldMappingsApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings/{customFieldId}")
public class UserCustomFieldMappingController implements UserCustomFieldMappingsApi {

  private final UserCustomFieldMappingService service;

  @Override
  @GetMapping
  public ResponseEntity<UserCustomFieldMappingsDTO> getUserCustomFieldMappingsByServerId(@PathVariable UUID centralServerId,
                                                                                         @PathVariable UUID customFieldId,
                                                                                         Integer offset, Integer limit) {

    var mappings = service.getAllMappings(centralServerId, customFieldId, offset, limit);

    return ResponseEntity.ok(mappings);
  }

  @Override
  @PutMapping
  public ResponseEntity<Void> updateUserCustomFieldMappings(@PathVariable UUID centralServerId,
                                                            @PathVariable UUID customFieldId,
                                                            UserCustomFieldMappingsDTO userCustomFieldMappingsDTO) {

    service.updateAllMappings(centralServerId, customFieldId, userCustomFieldMappingsDTO);

    return ResponseEntity.noContent().build();
  }
}
