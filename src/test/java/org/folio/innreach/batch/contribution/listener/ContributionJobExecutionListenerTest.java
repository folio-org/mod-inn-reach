package org.folio.innreach.batch.contribution.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.batch.contribution.ContributionJobContext.CENTRAL_SERVER_ID_KEY;
import static org.folio.innreach.dto.ContributionDTO.StatusEnum.COMPLETE;
import static org.folio.innreach.fixture.ContributionFixture.createContribution;
import static org.folio.innreach.fixture.ContributionFixture.mapper;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;

import org.folio.innreach.domain.service.ContributionService;

@ExtendWith(MockitoExtension.class)
class ContributionJobExecutionListenerTest {

  private static final String CENTRAL_SERVER_ID = UUID.randomUUID().toString();

  @Mock
  private ContributionService contributionService;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private JobExecution jobExecution;

  @InjectMocks
  private ContributionJobExecutionListener listener;

  @Test
  void beforeJob() {
    var contribution = mapper.toDTO(createContribution());

    when(jobExecution.getJobParameters().getString(CENTRAL_SERVER_ID_KEY)).thenReturn(CENTRAL_SERVER_ID);
    when(contributionService.getCurrent(any(UUID.class))).thenReturn(contribution);

    listener.beforeJob(jobExecution);

    verify(contributionService).getCurrent(any(UUID.class));
  }

  @Test
  void shouldFailBeforeJob() {
    var contribution = mapper.toDTO(createContribution());
    contribution.setStatus(COMPLETE);

    when(jobExecution.getJobParameters().getString(CENTRAL_SERVER_ID_KEY)).thenReturn(CENTRAL_SERVER_ID);
    when(contributionService.getCurrent(any(UUID.class))).thenReturn(contribution);

    assertThatThrownBy(() -> listener.beforeJob(jobExecution))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Initial contribution is not running");
  }

  @Test
  void afterJob() {
    when(jobExecution.getJobParameters().getString(CENTRAL_SERVER_ID_KEY)).thenReturn(CENTRAL_SERVER_ID);

    listener.afterJob(jobExecution);

    verify(contributionService).completeContribution(any(UUID.class));
  }
}
