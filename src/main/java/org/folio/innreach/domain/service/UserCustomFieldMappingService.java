package org.folio.innreach.domain.service;

import org.folio.innreach.dto.UserCustomFieldMappingDTO;

import java.util.UUID;

public interface UserCustomFieldMappingService {
  UserCustomFieldMappingDTO getMapping(UUID centralServerId);

  UserCustomFieldMappingDTO createMapping(UUID centralServerId, UserCustomFieldMappingDTO userCustomFieldMappingDTO);

  UserCustomFieldMappingDTO updateMapping(UUID centralServerId, UserCustomFieldMappingDTO userCustomFieldMappingDTO);
}
