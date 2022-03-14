package org.folio.innreach.domain.service;

import org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration;

public interface VisiblePatronFieldConfigurationService {
  VisiblePatronFieldConfiguration getByCentralCode(String centralServerCode);
}
