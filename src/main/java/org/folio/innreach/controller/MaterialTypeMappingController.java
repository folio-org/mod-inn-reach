package org.folio.innreach.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.dto.MaterialTypeMappingDTO;
import org.folio.innreach.dto.MaterialTypeMappingsDTO;
import org.folio.innreach.rest.resource.MaterialTypeMappingsApi;

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/inn-reach/central-servers")
public class MaterialTypeMappingController implements MaterialTypeMappingsApi {

  private final MaterialTypeMappingService materialTypeMappingService;

  @Override
  @GetMapping("/{centralServerId}/material-type-mappings")
  public ResponseEntity<MaterialTypeMappingsDTO> getMaterialTypeMappingsByServerId(@PathVariable UUID centralServerId,
      Integer offset, Integer limit) {

    var mappings = materialTypeMappingService.getAllMappings(centralServerId, offset, limit);

    return ResponseEntity.ok(mappings);
  }

  @Override
  @GetMapping("/{centralServerId}/material-type-mappings/{id}")
  public ResponseEntity<MaterialTypeMappingDTO> getMaterialTypeMappingById(@PathVariable UUID centralServerId,
      @PathVariable UUID id) {

    var mapping = materialTypeMappingService.getMapping(centralServerId, id);

    return ResponseEntity.ok(mapping);
  }

  @Override
  @PostMapping("/{centralServerId}/material-type-mappings")
  public ResponseEntity<MaterialTypeMappingDTO> postMaterialTypeMapping(@PathVariable UUID centralServerId,
      MaterialTypeMappingDTO materialTypeMappingDTO) {

    var mapping = materialTypeMappingService.createMapping(centralServerId, materialTypeMappingDTO);

    return ResponseEntity.status(HttpStatus.CREATED).body(mapping);
  }

  @Override
  @PutMapping("/{centralServerId}/material-type-mappings")
  public ResponseEntity<Void> updateMaterialTypeMappings(@PathVariable UUID centralServerId,
      MaterialTypeMappingsDTO materialTypeMappingsDTO) {

    materialTypeMappingService.updateAllMappings(centralServerId, materialTypeMappingsDTO);

    return ResponseEntity.noContent().build();
  }

  @Override
  @PutMapping("/{centralServerId}/material-type-mappings/{id}")
  public ResponseEntity<Void> updateMaterialTypeMapping(@PathVariable UUID centralServerId, @PathVariable UUID id,
      MaterialTypeMappingDTO materialTypeMappingDTO) {

    materialTypeMappingService.updateMapping(centralServerId, id, materialTypeMappingDTO);

    return ResponseEntity.noContent().build();
  }

  @Override
  @DeleteMapping("/{centralServerId}/material-type-mappings/{id}")
  public ResponseEntity<Void> deleteMaterialTypeMapping(@PathVariable UUID centralServerId, @PathVariable UUID id) {
    materialTypeMappingService.deleteMapping(centralServerId, id);

    return ResponseEntity.noContent().build();
  }

}
