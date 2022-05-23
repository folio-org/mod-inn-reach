package org.folio.innreach.domain.service;

import java.util.Optional;
import java.util.UUID;

import org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration;
import org.folio.innreach.dto.VisiblePatronFieldConfigurationDTO;

public interface VisiblePatronFieldConfigurationService {
  Optional<VisiblePatronFieldConfiguration> getByCentralCode(String centralServerCode);

  VisiblePatronFieldConfigurationDTO get(UUID centralServerId);

  VisiblePatronFieldConfigurationDTO create(UUID centralServerId, VisiblePatronFieldConfigurationDTO dto);

  VisiblePatronFieldConfigurationDTO update(UUID centralServerId, VisiblePatronFieldConfigurationDTO dto);
}
