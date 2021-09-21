package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.entity.UserCustomFieldMapping;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.UserCustomFieldMappingService;
import org.folio.innreach.dto.UserCustomFieldMappingDTO;
import org.folio.innreach.mapper.UserCustomFieldMappingMapper;
import org.folio.innreach.repository.UserCustomFieldMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;

@RequiredArgsConstructor
@Service
@Transactional
public class UserCustomFieldMappingServiceImpl implements UserCustomFieldMappingService {

  private final UserCustomFieldMappingRepository repository;
  private final UserCustomFieldMappingMapper mapper;

  private static final String TEXT_USER_CUSTOM_FIELD_MAPPING_NOT_FOUND = "User Custom Field Mapping not found: centralServerId = ";

  @Override
  @Transactional(readOnly = true)
  public UserCustomFieldMappingDTO getMapping(UUID centralServerId) {
    var mapping = repository.findOneByCentralServerId(centralServerId);
    return mapping.map(mapper::toDTO).orElseThrow(()
      -> new EntityNotFoundException(TEXT_USER_CUSTOM_FIELD_MAPPING_NOT_FOUND + centralServerId));
  }

  @Override
  public UserCustomFieldMappingDTO createMapping(UUID centralServerId, UserCustomFieldMappingDTO userCustomFieldMappingDTO) {
    var mapping = mapper.toEntity(userCustomFieldMappingDTO);
    mapping.setCentralServer(centralServerRef(centralServerId));
    var created = repository.save(mapping);
    return mapper.toDTO(created);
  }

  @Override
  public UserCustomFieldMappingDTO updateMapping(UUID centralServerId, UserCustomFieldMappingDTO userCustomFieldMappingDTO) {
    var mapping = repository.findOneByCentralServerId(centralServerId).orElseThrow(
      () -> new EntityNotFoundException(TEXT_USER_CUSTOM_FIELD_MAPPING_NOT_FOUND + centralServerId));

    var updated = mapper.toEntity(userCustomFieldMappingDTO);
    copyData(updated, mapping);

    repository.save(mapping);

    return mapper.toDTO(mapping);
  }

  private void copyData(UserCustomFieldMapping from, UserCustomFieldMapping to) {
    to.setCustomFieldId(from.getCustomFieldId());
    to.setConfiguredOptions(from.getConfiguredOptions());
  }
}
