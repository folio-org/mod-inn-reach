package org.folio.innreach.domain.service.impl;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration;
import org.folio.innreach.domain.service.VisiblePatronFieldConfigurationService;
import org.folio.innreach.repository.VisiblePatronFieldConfigurationRepository;

@Service
@RequiredArgsConstructor
public class VisiblePatronFieldConfigurationServiceImpl implements VisiblePatronFieldConfigurationService {
  private final VisiblePatronFieldConfigurationRepository repository;

  @Override
  public Optional<VisiblePatronFieldConfiguration> getByCentralCode(String centralServerCode) {
    return repository.findByCentralServerCode(centralServerCode);
  }
}
