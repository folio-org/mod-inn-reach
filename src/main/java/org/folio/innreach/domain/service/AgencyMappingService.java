package org.folio.innreach.domain.service;

import org.folio.innreach.dto.AgencyLocationMappingDTO;

import java.util.UUID;

public interface AgencyMappingService {

  AgencyLocationMappingDTO getMapping(UUID centralServerId);

  AgencyLocationMappingDTO updateMapping(UUID centralServerId, AgencyLocationMappingDTO mapping);

}
