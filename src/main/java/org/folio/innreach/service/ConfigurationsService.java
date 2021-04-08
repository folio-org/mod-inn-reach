package org.folio.innreach.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.dto.ModuleConfiguration;
import org.folio.innreach.dto.ModuleConfigurations;
import org.folio.innreach.domain.entity.Configuration;
import org.folio.innreach.mapper.ConfigurationsMapper;
import org.folio.innreach.repository.ConfigurationsRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
@Log4j2
public class ConfigurationsService {

  private final ConfigurationsRepository configurationsRepository;
  private final ConfigurationsMapper configurationsMapper;

  public void deleteConfigurationById(String configId) {
    var id = UUID.fromString(configId);

    configurationsRepository.deleteById(id);
  }

  public ModuleConfiguration getConfigurationById(String configId) {
    var id = UUID.fromString(configId);

    return configurationsRepository.findById(id).map(configurationsMapper::mapEntityToDto).orElse(null);
  }

  public ModuleConfigurations getConfigurations(Integer offset, Integer limit) {
    var configurationList = configurationsRepository.findAll(new OffsetRequest(offset, limit));

    return configurationsMapper.mapEntitiesToRemoteConfigCollection(configurationList);
  }

  public ModuleConfiguration postConfiguration(ModuleConfiguration moduleConfiguration) {
    if (isNull(moduleConfiguration.getId())) {
      moduleConfiguration.id(UUID.randomUUID().toString());
    }
    var configuration = configurationsMapper.mapDtoToEntity(moduleConfiguration);
    configuration.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));

    return configurationsMapper.mapEntityToDto(configurationsRepository.save(configuration));
  }

  public ModuleConfiguration createOrUpdateConfiguration(ModuleConfiguration moduleConfiguration) {
    var configuration = configurationsMapper.mapDtoToEntity(moduleConfiguration);
    if (isNull(configuration.getId())) {
      configuration.setId(UUID.randomUUID());
      if (isNull(configuration.getCreatedDate())) {
        configuration.setCreatedDate(Timestamp.valueOf(LocalDateTime.now()));
      }
    } else {
      configuration = copyForUpdate(configurationsRepository.getOne(configuration.getId()), configuration);
    }
    return configurationsMapper.mapEntityToDto(configurationsRepository.save(configuration));
  }

  private Configuration copyForUpdate(Configuration dest, Configuration source) {
    dest.setProviderName(source.getProviderName());
    dest.setUrl(source.getUrl());
    dest.setAccessionDelay(source.getAccessionDelay());
    dest.setAccessionTimeUnit(source.getAccessionTimeUnit());
    dest.setUpdatedByUserId(source.getUpdatedByUserId());
    dest.setUpdatedByUsername(source.getUpdatedByUsername());
    var ud = source.getUpdatedDate();
    dest.setUpdatedDate(ud != null ? ud : Timestamp.valueOf(LocalDateTime.now()));
    return dest;
  }
}
