package org.folio.innreach.domain.service.impl;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.LocationsClient;
import org.folio.innreach.domain.dto.folio.inventorystorage.LocationDTO;

@Service
@RequiredArgsConstructor
public class FolioLocationServiceImpl implements FolioLocationService {

  private static final String LOCATION_LIBRARY_MAPPING_CACHE = "folioLocationLibraryMappings";
  private static final int FETCH_LIMIT = 2000;

  private final LocationsClient locationsClient;

  @Cacheable(value = LOCATION_LIBRARY_MAPPING_CACHE)
  @Override
  public Map<UUID, UUID> getLocationLibraryMappings() {
    return locationsClient.getLocations(FETCH_LIMIT).getResult().stream()
      .collect(toMap(LocationDTO::getId, LocationDTO::getLibraryId));
  }

  @Override
  public LocationDTO getLocationById(UUID locationId) {
    return locationsClient.getLocationById(locationId);
  }

}
