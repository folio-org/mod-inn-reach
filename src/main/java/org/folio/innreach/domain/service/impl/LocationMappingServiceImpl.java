package org.folio.innreach.domain.service.impl;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.service.LocationMappingService;
import org.folio.innreach.dto.LocationMappingsDTO;

@RequiredArgsConstructor
@Service
@Transactional
public class LocationMappingServiceImpl implements LocationMappingService {

  @Override
  @Transactional(readOnly = true)
  public LocationMappingsDTO getAllMappings(UUID centralServerId, UUID libraryId, int offset, int limit) {
    return null;
  }

  @Override
  public LocationMappingsDTO updateAllMappings(UUID centralServerId, UUID libraryId,
      LocationMappingsDTO locationMappingsDTO) {
    return null;
  }

}
