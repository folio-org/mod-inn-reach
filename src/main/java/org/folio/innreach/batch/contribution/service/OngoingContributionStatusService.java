package org.folio.innreach.batch.contribution.service;

import org.folio.innreach.domain.entity.ContributionStatus;
import org.folio.innreach.domain.entity.OngoingContributionStatus;

public interface OngoingContributionStatusService {

  void updateOngoingContribution(OngoingContributionStatus ongoingContributionStatus,
                            String errorMsg, ContributionStatus status);
  void updateOngoingContribution(OngoingContributionStatus ongoingContributionStatus,
                                 ContributionStatus status);
}
