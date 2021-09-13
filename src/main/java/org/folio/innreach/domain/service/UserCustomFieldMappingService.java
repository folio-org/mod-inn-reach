package org.folio.innreach.domain.service;

import org.folio.innreach.dto.UserCustomFieldMappingsDTO;

import java.util.UUID;

public interface UserCustomFieldMappingService {
  UserCustomFieldMappingsDTO getAllMappings(UUID centralServerId, UUID customFieldId, Integer offset, Integer limit);

  UserCustomFieldMappingsDTO updateAllMappings(UUID centralServerId, UUID customFieldId, UserCustomFieldMappingsDTO userCustomFieldMappingsDTO);
}
