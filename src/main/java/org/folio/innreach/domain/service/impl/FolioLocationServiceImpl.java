package org.folio.innreach.domain.service.impl;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.LocationsClient;
import org.folio.innreach.domain.dto.folio.inventorystorage.LocationDTO;

@Log4j2
@Service
@RequiredArgsConstructor
public class FolioLocationServiceImpl implements FolioLocationService {

  private static final String LOCATION_LIBRARY_MAPPING_CACHE = "location-libraries";
  private static final int FETCH_LIMIT = 2000;

  private final LocationsClient locationsClient;

  @Override
  @Cacheable(value = LOCATION_LIBRARY_MAPPING_CACHE, key = "@folioExecutionContext.tenantId")
  public Map<UUID, UUID> getLocationLibraryMappings() {
    log.debug("getLocationLibraryMappings:: no parameter");
    return locationsClient.getLocations(FETCH_LIMIT).getResult().stream()
      .collect(toMap(LocationDTO::getId, LocationDTO::getLibraryId));
  }

}
