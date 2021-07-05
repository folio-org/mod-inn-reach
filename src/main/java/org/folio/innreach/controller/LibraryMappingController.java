package org.folio.innreach.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.LibraryMappingService;
import org.folio.innreach.dto.LibraryMappingsDTO;
import org.folio.innreach.rest.resource.LibraryMappingsApi;

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/inn-reach/central-servers")
public class LibraryMappingController implements LibraryMappingsApi {

  private final LibraryMappingService libraryMappingService;

  @Override
  @GetMapping("/{centralServerId}/libraries/location-mappings")
  public ResponseEntity<LibraryMappingsDTO> getLibraryMappingsByServerId(@PathVariable UUID centralServerId,
      Integer offset, Integer limit) {

    var mappings = libraryMappingService.getAllMappings(centralServerId, offset, limit);

    return ResponseEntity.ok(mappings);
  }

  @Override
  @PutMapping("/{centralServerId}/libraries/location-mappings")
  public ResponseEntity<Void> putLibraryMappings(@PathVariable UUID centralServerId,
      LibraryMappingsDTO libraryMappingsDTO) {

    libraryMappingService.updateAllMappings(centralServerId, libraryMappingsDTO);

    return ResponseEntity.noContent().build();
  }

}