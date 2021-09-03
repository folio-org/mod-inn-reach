package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus;
import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.dto.ContributionErrorDTO;
import org.folio.innreach.dto.ContributionsDTO;

public interface ContributionService {

  ContributionDTO getCurrent(UUID centralServerId);

  ContributionsDTO getHistory(UUID centralServerId, int offset, int limit);

  void updateContributionStats(UUID centralServerId, ContributionDTO contribution);

  void startInitialContribution(UUID centralServerId);

  void completeContribution(UUID centralServerId);

  ContributionItemCirculationStatus getItemCirculationStatus(UUID centralServerId, UUID itemId);

  void logContributionError(UUID contributionId, ContributionErrorDTO error);

}
