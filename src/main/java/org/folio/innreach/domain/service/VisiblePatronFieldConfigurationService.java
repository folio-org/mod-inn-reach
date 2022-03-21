package org.folio.innreach.domain.service;

import java.util.Optional;

import org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration;

public interface VisiblePatronFieldConfigurationService {
  Optional<VisiblePatronFieldConfiguration> getByCentralCode(String centralServerCode);
}
