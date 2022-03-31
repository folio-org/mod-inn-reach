package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.VisiblePatronFieldConfigurationService;
import org.folio.innreach.dto.VisiblePatronFieldConfigurationDTO;
import org.folio.innreach.mapper.VisiblePatronFieldConfigurationMapper;
import org.folio.innreach.repository.VisiblePatronFieldConfigurationRepository;

@Service
@RequiredArgsConstructor
public class VisiblePatronFieldConfigurationServiceImpl implements VisiblePatronFieldConfigurationService {
  private final VisiblePatronFieldConfigurationRepository repository;
  private final VisiblePatronFieldConfigurationMapper mapper;

  private static final String TEXT_CONFIGURATION_NOT_FOUND = "Visible Patron Field Configuration not found: centralServerId = ";

  @Transactional(readOnly = true)
  @Override
  public Optional<VisiblePatronFieldConfiguration> getByCentralCode(String centralServerCode) {
    return repository.findByCentralServerCode(centralServerCode);
  }

  @Transactional(readOnly = true)
  @Override
  public VisiblePatronFieldConfigurationDTO get(UUID centralServerId) {
    var config = repository.findOneByCentralServerId(centralServerId);
    return config.map(mapper::toDTO).orElseThrow(()
      -> new EntityNotFoundException(TEXT_CONFIGURATION_NOT_FOUND + centralServerId));
  }

  @Override
  public VisiblePatronFieldConfigurationDTO create(UUID centralServerId, VisiblePatronFieldConfigurationDTO dto) {
    var config = mapper.toEntity(dto);
    config.setCentralServer(centralServerRef(centralServerId));
    var created = repository.save(config);
    return mapper.toDTO(created);
  }

  @Override
  public VisiblePatronFieldConfigurationDTO update(UUID centralServerId, VisiblePatronFieldConfigurationDTO dto) {
    var config = repository.findOneByCentralServerId(centralServerId).orElseThrow(
      () -> new EntityNotFoundException(TEXT_CONFIGURATION_NOT_FOUND + centralServerId));

    if(fieldConfigurationIsEmpty(dto)){
      repository.delete(config);
      return null;
    }

    var updated = mapper.toEntity(dto);
    copyData(updated, config);

    repository.save(config);

    return mapper.toDTO(config);
  }

  private boolean fieldConfigurationIsEmpty(VisiblePatronFieldConfigurationDTO dto) {
    return dto.getFields().isEmpty() && dto.getUserCustomFields().isEmpty();
  }

  private void copyData(VisiblePatronFieldConfiguration from, VisiblePatronFieldConfiguration to) {
    to.setFields(from.getFields());
    to.setUserCustomFields(from.getUserCustomFields());
  }
}
