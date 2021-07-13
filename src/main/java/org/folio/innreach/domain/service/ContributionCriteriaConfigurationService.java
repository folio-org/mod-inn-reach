package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.ContributionCriteriaDTO;

public interface ContributionCriteriaConfigurationService {

  ContributionCriteriaDTO createCriteria(UUID centralServerId, ContributionCriteriaDTO criteriaDTO);

  ContributionCriteriaDTO getCriteria(UUID centralServerId);

  ContributionCriteriaDTO updateCriteria(UUID centralServerId, ContributionCriteriaDTO criteriaDTO);

  void deleteCriteria(UUID centralServerId);

}
