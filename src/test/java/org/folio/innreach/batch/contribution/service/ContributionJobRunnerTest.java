package org.folio.innreach.batch.contribution.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.ContributionFixture.createContributionJobContext;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.ContributionFixture.createInstanceView;
import static org.folio.innreach.fixture.TestUtil.createNoRetryTemplate;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.retry.support.RetryTemplate;

import org.folio.innreach.batch.KafkaItemReader;
import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.batch.contribution.IterationEventReaderFactory;
import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.batch.contribution.listener.ContributionJobStatsListener;
import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.config.props.FolioEnvironment;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.domain.service.RecordContributionService;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@ExtendWith(MockitoExtension.class)
class ContributionJobRunnerTest {

  private static final ContributionJobContext JOB_CONTEXT = createContributionJobContext();
  private static final UUID JOB_ID = JOB_CONTEXT.getIterationJobId();
  private static final UUID CENTRAL_SERVER_ID = JOB_CONTEXT.getCentralServerId();

  @Qualifier("instanceExceptionListener")
  @Mock
  private ContributionExceptionListener instanceExceptionListener;
  @Qualifier("itemExceptionListener")
  @Mock
  private ContributionExceptionListener itemExceptionListener;
  @Mock
  private ContributionJobStatsListener statsListener;
  @Mock
  private InventoryViewService inventoryViewService;
  @Mock
  private ContributionValidationService validationService;
  @Mock
  private RecordContributionService recordContributor;
  @Mock
  private ContributionJobProperties jobProperties;
  @Mock
  private KafkaProperties kafkaProperties;
  @Mock
  private FolioEnvironment folioEnv;
  @Mock
  private ContributionService contributionService;
  @Mock
  private IterationEventReaderFactory factory;
  @Mock
  private KafkaItemReader<String, InstanceIterationEvent> reader;
  @Spy
  private RetryTemplate retryTemplate = createNoRetryTemplate();

  @InjectMocks
  private ContributionJobRunner jobRunner;

  @Test
  void shouldRunJob() {
    var event = InstanceIterationEvent.of(JOB_ID, "test", "test", UUID.randomUUID());

    when(factory.createReader(any())).thenReturn(reader);
    when(reader.read())
      .thenReturn(event)
      .thenReturn(null);
    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Item.class))).thenReturn(true);
    when(inventoryViewService.getInstance(any())).thenReturn(createInstanceView().toInstance());

    jobRunner.runInitialContribution(JOB_CONTEXT);

    verify(reader, times(2)).read();
    verify(recordContributor).contributeInstance(any(), any());
    verify(recordContributor).contributeItems(any(), any(), anyList());
  }

  @Test
  void shouldRunJob_noInstanceItems() {
    var event = InstanceIterationEvent.of(JOB_ID, "test", "test", UUID.randomUUID());
    Instance instance = createInstance();
    instance.setItems(null);

    when(factory.createReader(any())).thenReturn(reader);
    when(reader.read())
      .thenReturn(event)
      .thenReturn(null);
    when(inventoryViewService.getInstance(any())).thenReturn(instance);

    jobRunner.runInitialContribution(JOB_CONTEXT);

    verify(reader, times(2)).read();
    verify(recordContributor).isContributed(eq(CENTRAL_SERVER_ID), eq(instance));
  }

  @Test
  void shouldRunJob_deContributeIneligibleInstance() {
    var event = InstanceIterationEvent.of(JOB_ID, "test", "test", UUID.randomUUID());
    Instance instance = createInstance();
    instance.setItems(null);

    when(factory.createReader(any())).thenReturn(reader);
    when(reader.read())
      .thenReturn(event)
      .thenReturn(null);
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(recordContributor.isContributed(any(), any())).thenReturn(true);

    jobRunner.runInitialContribution(JOB_CONTEXT);

    verify(reader, times(2)).read();
    verify(recordContributor).deContributeInstance(eq(CENTRAL_SERVER_ID), eq(instance));
  }

  @Test
  void shouldRunJob_noInstances() {
    var event = InstanceIterationEvent.of(JOB_ID, "test", "test", UUID.randomUUID());

    when(factory.createReader(any())).thenReturn(reader);
    when(reader.read())
      .thenReturn(event)
      .thenReturn(null);
    when(inventoryViewService.getInstance(any())).thenReturn(null);

    jobRunner.runInitialContribution(JOB_CONTEXT);

    verify(reader, times(2)).read();
    verify(inventoryViewService).getInstance(any());
    verifyNoMoreInteractions(recordContributor);
  }

  @Test
  void shouldRunJob_noEvents() {
    when(factory.createReader(any())).thenReturn(reader);
    when(reader.read()).thenReturn(null);

    jobRunner.runInitialContribution(JOB_CONTEXT);

    verify(reader).read();
    verifyNoInteractions(recordContributor);
  }

  @Test
  void throwsExceptionOnRead() {
    when(factory.createReader(any())).thenReturn(reader);
    String exceptionMsg = "test message";
    when(reader.read()).thenThrow(new RuntimeException(exceptionMsg));

    assertThatThrownBy(() -> jobRunner.runInitialContribution(JOB_CONTEXT))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining(exceptionMsg);

    verify(reader).read();
    verify(contributionService).completeContribution(JOB_CONTEXT.getContributionId());
    verifyNoInteractions(recordContributor);
  }

  @Test
  void shouldCancelJob() {
    jobRunner.cancelJobs();

    verify(contributionService).cancelAll();
  }

}
