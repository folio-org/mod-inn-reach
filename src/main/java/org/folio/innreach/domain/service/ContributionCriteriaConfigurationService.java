package org.folio.innreach.domain.service;

import org.folio.innreach.domain.dto.ContributionCriteriaConfigurationDTO;

import java.util.List;
import java.util.UUID;

public interface ContributionCriteriaConfigurationService {
  ContributionCriteriaConfigurationDTO create(ContributionCriteriaConfigurationDTO criteriaConfigurationDTO);
  ContributionCriteriaConfigurationDTO get(UUID centralServerId);
  List<ContributionCriteriaConfigurationDTO> getAll(UUID centralServerId);
  ContributionCriteriaConfigurationDTO update(ContributionCriteriaConfigurationDTO criteriaConfigurationDTO);
  void delete(UUID centralServerId);
}
