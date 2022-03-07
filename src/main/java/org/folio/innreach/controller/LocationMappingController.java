package org.folio.innreach.controller;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.LocationMappingService;
import org.folio.innreach.dto.LocationMappingForAllLibrariesDTO;
import org.folio.innreach.dto.LocationMappingsForOneLibraryDTO;
import org.folio.innreach.rest.resource.LocationMappingsApi;

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/inn-reach/central-servers")
public class LocationMappingController implements LocationMappingsApi {

  private final LocationMappingService libraryMappingService;


  @Override
  @GetMapping("/{centralServerId}/libraries/{libraryId}/locations/location-mappings")
  public ResponseEntity<LocationMappingsForOneLibraryDTO> getLocationMappingsByServerId(@PathVariable UUID centralServerId,
                                                                                        @PathVariable UUID libraryId, Integer offset, Integer limit) {

    var mappings = libraryMappingService.getMappingsByLibraryId(centralServerId, libraryId, offset, limit);

    return ResponseEntity.ok(mappings);
  }

  @Override
  @GetMapping("/{centralServerId}/libraries/locations/location-mappings")
  public ResponseEntity<List<LocationMappingForAllLibrariesDTO>> getLocationMappingsForAllLibrariesByServerId(@PathVariable UUID centralServerId) {

    var mappings = libraryMappingService.getAllMappings(centralServerId);

    return ResponseEntity.ok(mappings);
  }

  @Override
  @PutMapping("/{centralServerId}/libraries/{libraryId}/locations/location-mappings")
  public ResponseEntity<Void> putLocationMappings(@PathVariable UUID centralServerId, @PathVariable UUID libraryId,
      LocationMappingsForOneLibraryDTO locationMappingsDTO) {

    libraryMappingService.updateAllMappings(centralServerId, libraryId, locationMappingsDTO);

    return ResponseEntity.noContent().build();
  }

}
