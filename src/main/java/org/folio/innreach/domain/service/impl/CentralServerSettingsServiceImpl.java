package org.folio.innreach.domain.service.impl;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.CentralServerSettingsService;
import org.folio.innreach.dto.CentralServerSettingsDTO;
import org.folio.innreach.mapper.CentralServerSettingsMapper;
import org.folio.innreach.repository.CentralServerSettingsRepository;

@RequiredArgsConstructor
@Log4j2
@Service
public class CentralServerSettingsServiceImpl implements CentralServerSettingsService {

  private final CentralServerSettingsRepository centralServerSettingsRepository;
  private final CentralServerSettingsMapper centralServerSettingsMapper;

  @Override
  @Transactional
  public CentralServerSettingsDTO createCentralServerSettings(CentralServerSettingsDTO centralServerSettingsDTO) {
    var centralServerSettings = centralServerSettingsMapper.mapToCentralServerSettings(centralServerSettingsDTO);
    var savedCentralServerSettings = centralServerSettingsRepository.save(centralServerSettings);
    return centralServerSettingsMapper.mapToCentralServiceSettingsDTO(savedCentralServerSettings);
  }

  @Override
  @Transactional(readOnly = true)
  public CentralServerSettingsDTO getCentralServerSettingsByCentralServerId(UUID centralServerId) {
    var centralServerSettings = centralServerSettingsRepository.findByCentralServerId(centralServerId)
      .orElseThrow(() -> new EntityNotFoundException("Central server settings for central server with ID: " + centralServerId + " not found"));
    return centralServerSettingsMapper.mapToCentralServiceSettingsDTO(centralServerSettings);
  }

  @Override
  @Transactional
  public void updateCentralServerSettingsByCentralServerId(CentralServerSettingsDTO centralServerSettingsDTO) {
    var centralServerSettings = centralServerSettingsMapper.mapToCentralServerSettings(centralServerSettingsDTO);
    centralServerSettingsRepository.save(centralServerSettings);
  }
}
