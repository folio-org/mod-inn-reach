package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.dto.ContributionErrorDTO;
import org.folio.innreach.dto.ContributionsDTO;

public interface ContributionService {

  ContributionDTO getCurrent(UUID centralServerId);

  ContributionsDTO getHistory(UUID centralServerId, int offset, int limit);

  void updateContributionStats(UUID contributionId, ContributionDTO contribution);

  void startInitialContribution(UUID centralServerId);

  ContributionDTO createOngoingContribution(UUID centralServerId);

  ContributionDTO completeContribution(UUID contributionId);

  void cancelAll();

  void cancelCurrent(UUID centralServerId);

  void logContributionError(UUID contributionId, ContributionErrorDTO error);

  //void completeJobExecution(UUID jobExecutionId);

}
