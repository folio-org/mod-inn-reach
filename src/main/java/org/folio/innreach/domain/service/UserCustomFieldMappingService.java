package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.UserCustomFieldMappingsDTO;

public interface UserCustomFieldMappingService {
  UserCustomFieldMappingsDTO getAllMappings(UUID centralServerId, UUID customFieldId, Integer offset, Integer limit);

  UserCustomFieldMappingsDTO updateAllMappings(UUID centralServerId, UUID customFieldId, UserCustomFieldMappingsDTO userCustomFieldMappingsDTO);
}
