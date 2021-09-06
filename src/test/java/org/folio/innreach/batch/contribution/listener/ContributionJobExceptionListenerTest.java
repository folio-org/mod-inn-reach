package org.folio.innreach.batch.contribution.listener;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.dto.ContributionErrorDTO;
import org.folio.innreach.dto.Instance;

@ExtendWith(MockitoExtension.class)
class ContributionJobExceptionListenerTest {

  private static final UUID CONTRIBUTION_ID = UUID.randomUUID();

  @Mock
  private ContributionService contributionService;

  @Mock
  private ContributionJobContext context;

  @InjectMocks
  private ContributionJobExceptionListener listener;

  @Test
  void shouldLogOnReadError() {
    when(context.getContributionId()).thenReturn(CONTRIBUTION_ID);

    listener.onReadError(new IllegalArgumentException("test exception"));

    verify(contributionService).logContributionError(any(UUID.class), any(ContributionErrorDTO.class));
  }

  @Test
  void shouldLogOnProcessError() {
    when(context.getContributionId()).thenReturn(CONTRIBUTION_ID);

    listener.onProcessError(new InstanceIterationEvent(), new IllegalArgumentException("test exception"));

    verify(contributionService).logContributionError(any(UUID.class), any(ContributionErrorDTO.class));
  }

  @Test
  void shouldLogOnWriteError() {
    when(context.getContributionId()).thenReturn(CONTRIBUTION_ID);

    listener.onWriteError(
      new IllegalArgumentException("test exception"), singletonList(new Instance()));

    verify(contributionService).logContributionError(any(UUID.class), any(ContributionErrorDTO.class));
  }
}
