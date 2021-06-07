package org.folio.innreach.controller;

import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.dto.CentralServerDTO;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.dto.MaterialTypeMappingDTO;
import org.folio.innreach.dto.MaterialTypeMappingsDTO;
import org.folio.innreach.rest.resource.CentralServersApi;

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/inn-reach/central-servers")
public class CentralServerController implements CentralServersApi {

  private final CentralServerService centralServerService;
  private final MaterialTypeMappingService materialTypeMappingService;


  @PostMapping
  public ResponseEntity<CentralServerDTO> createCentralServer(@Valid @RequestBody CentralServerDTO centralServerDTO) {
    var createdCentralServer = centralServerService.createCentralServer(centralServerDTO);

    return ResponseEntity.status(HttpStatus.CREATED).body(createdCentralServer);
  }

  @GetMapping
  public ResponseEntity<List<CentralServerDTO>> getAllCentralServers() {
    var allCentralServers = centralServerService.getAllCentralServers();

    return ResponseEntity.ok(allCentralServers);
  }

  @GetMapping("/{centralServerId}")
  public ResponseEntity<CentralServerDTO> getCentralServer(@PathVariable UUID centralServerId) {
    var centralServer = centralServerService.getCentralServer(centralServerId);

    return ResponseEntity.ok(centralServer);
  }

  @PutMapping("/{centralServerId}")
  public ResponseEntity<CentralServerDTO> updateCentralServer(@PathVariable UUID centralServerId,
      @RequestBody @Valid CentralServerDTO centralServerDTO) {
    var updatedCentralServer = centralServerService.updateCentralServer(centralServerId, centralServerDTO);

    return ResponseEntity.ok(updatedCentralServer);
  }

  @DeleteMapping("/{centralServerId}")
  public ResponseEntity<CentralServerDTO> deleteCentralServer(@PathVariable UUID centralServerId) {
    centralServerService.deleteCentralServer(centralServerId);

    return ResponseEntity.noContent().build();
  }

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
  @PutMapping("/{centralServerId}/material-type-mappings/{id}")
  public ResponseEntity<MaterialTypeMappingDTO> updateMaterialTypeMapping(@PathVariable UUID centralServerId,
      @PathVariable UUID id, MaterialTypeMappingDTO materialTypeMappingDTO) {

    var mapping = materialTypeMappingService.updateMapping(centralServerId, id, materialTypeMappingDTO);

    return ResponseEntity.ok(mapping);
  }

  @Override
  @DeleteMapping("/{centralServerId}/material-type-mappings/{id}")
  public ResponseEntity<Void> deleteMaterialTypeMapping(@PathVariable UUID centralServerId, @PathVariable UUID id) {
    materialTypeMappingService.deleteMapping(centralServerId, id);

    return ResponseEntity.noContent().build();
  }

}
