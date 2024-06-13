package org.folio.innreach.batch.contribution.service;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.entity.ContributionStatus;
import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.folio.innreach.repository.OngoingContributionStatusRepository;
import org.springframework.stereotype.Service;

import static org.folio.innreach.domain.entity.ContributionStatus.RETRY;

@Service
@AllArgsConstructor
@Log4j2
public class OngoingContributionStatusServiceImpl implements OngoingContributionStatusService {

  private final OngoingContributionStatusRepository ongoingContributionStatusRepository;
  @Override
  public void updateOngoingContribution(OngoingContributionStatus ongoingContributionStatus,
                                        String errorMsg, ContributionStatus status) {
    var existingError = ongoingContributionStatus.getError();
    ongoingContributionStatus.setError(existingError != null ? existingError + " | " + errorMsg : errorMsg);
    updateStatusAndSaveOngoingJob(ongoingContributionStatus, status);
  }

  @Override
  public void updateOngoingContribution(OngoingContributionStatus ongoingContributionStatus,
                                        ContributionStatus status) {
    updateStatusAndSaveOngoingJob(ongoingContributionStatus, status);
  }

  private void updateStatusAndSaveOngoingJob(OngoingContributionStatus ongoingContributionStatus,
                                             ContributionStatus status) {
    ongoingContributionStatus.setStatus(status);
    ongoingContributionStatus.setRetryAttempts(status.equals(RETRY) ?
      ongoingContributionStatus.getRetryAttempts() + 1 : ongoingContributionStatus.getRetryAttempts());
    ongoingContributionStatusRepository.save(ongoingContributionStatus);
    }
}
