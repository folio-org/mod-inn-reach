package org.folio.innreach.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.dto.ContributionCriteriaConfigurationDTO;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface ContributionCriteriaConfigurationService {
  ContributionCriteriaConfigurationDTO create(ContributionCriteriaConfigurationDTO criteriaConfigurationDTO);
  ContributionCriteriaConfigurationDTO get(UUID centralServerId);
  ContributionCriteriaConfigurationDTO update(ContributionCriteriaConfigurationDTO criteriaConfigurationDTO);
  void delete(UUID centralServerId);
}
