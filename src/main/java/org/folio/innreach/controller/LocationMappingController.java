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

import org.folio.innreach.domain.service.LocationMappingService;
import org.folio.innreach.dto.LocationMappingsDTO;
import org.folio.innreach.rest.resource.LocationMappingsApi;

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/inn-reach/central-servers")
public class LocationMappingController implements LocationMappingsApi {

  private final LocationMappingService libraryMappingService;


  @Override
  @GetMapping("/{centralServerId}/libraries/{libraryId}/locations/location-mappings")
  public ResponseEntity<LocationMappingsDTO> getLocationMappingsByServerId(@PathVariable UUID centralServerId,
      @PathVariable UUID libraryId, Integer offset, Integer limit) {

    var mappings = libraryMappingService.getAllMappings(centralServerId, libraryId, offset, limit);

    return ResponseEntity.ok(mappings);
  }

  @Override
  @GetMapping("/{centralServerId}/libraries/locations/location-mappings")
  public ResponseEntity<LocationMappingsDTO> getLocationMappingsForAllLibrariesByServerId(@PathVariable UUID centralServerId,
                                                                                          Integer offset, Integer limit) {

    var mappings = libraryMappingService.getAllMappingsForAllLibraries(centralServerId, offset, limit);

    return ResponseEntity.ok(mappings);
  }

  @Override
  @PutMapping("/{centralServerId}/libraries/{libraryId}/locations/location-mappings")
  public ResponseEntity<Void> putLocationMappings(@PathVariable UUID centralServerId, @PathVariable UUID libraryId,
      LocationMappingsDTO locationMappingsDTO) {

    libraryMappingService.updateAllMappings(centralServerId, libraryId, locationMappingsDTO);

    return ResponseEntity.noContent().build();
  }

}
