package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.UserCustomFieldMapping;
import org.folio.innreach.domain.service.UserCustomFieldMappingService;
import org.folio.innreach.dto.UserCustomFieldMappingsDTO;
import org.folio.innreach.mapper.UserCustomFieldMappingMapper;
import org.folio.innreach.repository.UserCustomFieldMappingRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
import static org.folio.innreach.domain.service.impl.ServiceUtils.initId;
import static org.folio.innreach.domain.service.impl.ServiceUtils.mergeAndSave;

@RequiredArgsConstructor
@Service
@Transactional
public class UserCustomFieldMappingServiceImpl implements UserCustomFieldMappingService {

  private final UserCustomFieldMappingRepository repository;
  private final UserCustomFieldMappingMapper mapper;

  @Override
  @Transactional(readOnly = true)
  public UserCustomFieldMappingsDTO getAllMappings(UUID centralServerId, UUID customFieldId, Integer offset, Integer limit) {
    var example = mappingExampleWithServerIdAndCustomFieldId(centralServerId, customFieldId);

    Page<UserCustomFieldMapping> mappings = repository.findAll(example, PageRequest.of(offset, limit));

    return mapper.toDTOCollection(mappings);
  }

  @Override
  public UserCustomFieldMappingsDTO updateAllMappings(UUID centralServerId, UUID customFieldId, UserCustomFieldMappingsDTO userCustomFieldMappingsDTO) {
    var stored = repository.findAll(mappingExampleWithServerIdAndCustomFieldId(
      centralServerId, customFieldId));

    var incoming = mapper.toEntities(userCustomFieldMappingsDTO.getUserCustomFieldMappings());
    var csRef = centralServerRef(centralServerId);
    incoming.forEach(setCentralServerRef(csRef).andThen(setCustomFieldId(customFieldId)).andThen(initId()));

    var saved = mergeAndSave(incoming, stored, repository, this::copyData);

    return mapper.toDTOCollection(saved);
  }

  private static Example<UserCustomFieldMapping> mappingExampleWithServerIdAndCustomFieldId(UUID centralServerId,
                                                                                            UUID customFieldId) {
    var toFind = new UserCustomFieldMapping();
    toFind.setCentralServer(centralServerRef(centralServerId));
    toFind.setCustomFieldId(customFieldId);

    return Example.of(toFind);
  }

  private void copyData(UserCustomFieldMapping from, UserCustomFieldMapping to) {
    to.setCustomFieldValue(from.getCustomFieldValue());
    to.setAgencyCode(from.getAgencyCode());
  }

  private static Consumer<UserCustomFieldMapping> setCentralServerRef(CentralServer centralServer) {
    return mapping -> mapping.setCentralServer(centralServer);
  }

  private static Consumer<UserCustomFieldMapping> setCustomFieldId(UUID customFieldId) {
    return mapping -> mapping.setCustomFieldId(customFieldId);
  }
}
