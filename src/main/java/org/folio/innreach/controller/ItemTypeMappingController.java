package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.service.ItemTypeMappingService;
import org.folio.innreach.dto.ItemTypeMappingsDTO;
import org.folio.innreach.rest.resource.ItemTypeMappingsApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inn-reach/central-servers")
public class ItemTypeMappingController implements ItemTypeMappingsApi {

  private final ItemTypeMappingService service;

  @Override
  @GetMapping("/{centralServerId}/item-type-mappings")
  public ResponseEntity<ItemTypeMappingsDTO> getItemTypeMappingsByServerId(@PathVariable UUID centralServerId,
                                                                           Integer offset, Integer limit) {

    var mappings = service.getAllMappings(centralServerId, offset, limit);

    return ResponseEntity.ok(mappings);
  }

  @Override
  @PutMapping("/{centralServerId}/item-type-mappings")
  public ResponseEntity<Void> updateItemTypeMappings(@PathVariable UUID centralServerId,
                                                       ItemTypeMappingsDTO itemTypeMappingsDTO) {

    service.updateAllMappings(centralServerId, itemTypeMappingsDTO);

    return ResponseEntity.noContent().build();
  }
}
