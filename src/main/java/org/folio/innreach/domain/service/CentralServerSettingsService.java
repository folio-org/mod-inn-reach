package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.CentralServerSettingsDTO;

public interface CentralServerSettingsService {

  CentralServerSettingsDTO createCentralServerSettings(CentralServerSettingsDTO centralServerSettingsDTO);

  CentralServerSettingsDTO getCentralServerSettingsByCentralServerId(UUID uuid);

  void updateCentralServerSettingsByCentralServerId(CentralServerSettingsDTO centralServerSettingsDTO);
}
