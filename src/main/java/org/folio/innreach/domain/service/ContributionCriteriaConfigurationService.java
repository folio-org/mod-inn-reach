package org.folio.innreach.domain.service;

import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface ContributionCriteriaConfigurationService {
  ContributionCriteriaDTO create(ContributionCriteriaDTO criteriaConfigurationDTO);
  ContributionCriteriaDTO get(UUID centralServerId);
  ContributionCriteriaDTO update(ContributionCriteriaDTO criteriaConfigurationDTO);
  void delete(UUID centralServerId);
}
