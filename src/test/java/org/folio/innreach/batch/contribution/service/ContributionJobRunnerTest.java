package org.folio.innreach.batch.contribution.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.ContributionFixture.createContributionJobContext;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.ContributionFixture.createInstanceView;
import static org.folio.innreach.fixture.ContributionFixture.createItem;
import static org.folio.innreach.fixture.TestUtil.createNoRetryTemplate;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Disabled;
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
import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.spring.FolioExecutionContext;

@ExtendWith(MockitoExtension.class)
class ContributionJobRunnerTest {

  private static final String MARC_RECORD_SOURCE = "MARC";
  private static final ContributionJobContext JOB_CONTEXT = createContributionJobContext();
  private static final String TENANT_ID = JOB_CONTEXT.getTenantId();
  private static final UUID CONTRIBUTION_ID = JOB_CONTEXT.getContributionId();
  private static final UUID ITERATION_JOB_ID = JOB_CONTEXT.getIterationJobId();
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
  private FolioExecutionContext folioContext;
  @Mock
  private ContributionService contributionService;
  @Mock
  private IterationEventReaderFactory factory;
  @Mock
  private KafkaItemReader<String, InstanceIterationEvent> reader;
  @Mock
  private InstanceIterationEvent event;
  @Mock
  private ContributionJobContext.Statistics statistics;
  @Spy
  private RetryTemplate retryTemplate = createNoRetryTemplate();

  @InjectMocks
  private ContributionJobRunner jobRunner;

  @Test
  void shouldRunJob() {
    var event = InstanceIterationEvent.of(ITERATION_JOB_ID, "test", "test", UUID.randomUUID());

    when(factory.createReader(any())).thenReturn(reader);
    when(reader.read())
      .thenReturn(event)
      .thenReturn(null);
    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Item.class))).thenReturn(true);
    when(inventoryViewService.getInstance(any())).thenReturn(createInstanceView().toInstance());

    jobRunner.runInitialContribution(JOB_CONTEXT, ContributionJobRunnerTest.this.event, statistics);

    verify(reader, times(2)).read();
    verify(recordContributor).contributeInstance(any(), any());
    verify(recordContributor).contributeItems(any(), any(), anyList());
  }

  @Test
  void shouldRunJob_noInstanceItems() {
    var event = InstanceIterationEvent.of(ITERATION_JOB_ID, "test", "test", UUID.randomUUID());
    Instance instance = createInstance();
    instance.setItems(null);

    when(factory.createReader(any())).thenReturn(reader);
    when(reader.read())
      .thenReturn(event)
      .thenReturn(null);
    when(inventoryViewService.getInstance(any())).thenReturn(instance);

    jobRunner.runInitialContribution(JOB_CONTEXT, ContributionJobRunnerTest.this.event, statistics);

    verify(reader, times(2)).read();
    verify(recordContributor).isContributed(CENTRAL_SERVER_ID, instance);
  }

  @Test
  void shouldRunJob_deContributeIneligibleInstance() {
    var event = InstanceIterationEvent.of(ITERATION_JOB_ID, "test", "test", UUID.randomUUID());
    Instance instance = createInstance();
    instance.setItems(null);

    when(factory.createReader(any())).thenReturn(reader);
    when(reader.read())
      .thenReturn(event)
      .thenReturn(null);
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(recordContributor.isContributed(any(), any())).thenReturn(true);

    jobRunner.runInitialContribution(JOB_CONTEXT, ContributionJobRunnerTest.this.event, statistics);

    verify(reader, times(2)).read();
    verify(recordContributor).deContributeInstance(CENTRAL_SERVER_ID, instance);
  }

  @Test
  void shouldRunJob_noInstances() {
    var event = InstanceIterationEvent.of(ITERATION_JOB_ID, "test", "test", UUID.randomUUID());

    when(factory.createReader(any())).thenReturn(reader);
    when(reader.read())
      .thenReturn(event)
      .thenReturn(null);
    when(inventoryViewService.getInstance(any())).thenReturn(null);

    jobRunner.runInitialContribution(JOB_CONTEXT, ContributionJobRunnerTest.this.event, statistics);

    verify(reader, times(2)).read();
    verify(inventoryViewService).getInstance(any());
    verifyNoMoreInteractions(recordContributor);
  }

  @Test
  void shouldRunJob_noEvents() {
    when(factory.createReader(any())).thenReturn(reader);
    when(reader.read()).thenReturn(null);

    jobRunner.runInitialContribution(JOB_CONTEXT, event, statistics);

    verify(reader).read();
    verifyNoInteractions(recordContributor);
  }

  @Test
  @Disabled
  void throwsExceptionOnRead() {
    when(factory.createReader(any())).thenReturn(reader);
    String exceptionMsg = "test message";
    when(reader.read()).thenThrow(new RuntimeException(exceptionMsg));

    assertThatThrownBy(() -> jobRunner.runInitialContribution(JOB_CONTEXT, event, statistics))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining(exceptionMsg);

    verify(reader).read();
    verify(contributionService).completeContribution(CONTRIBUTION_ID);
    verifyNoInteractions(recordContributor);
  }

  @Test
  void shouldCancelJobs() {
    jobRunner.cancelJobs();

    verify(contributionService).cancelAll();
  }

  @Test
  void runInstanceContribution_shouldContribute() {
    var instance = createInstance();
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Item.class))).thenReturn(true);
    when(contributionService.createOngoingContribution(any())).thenReturn(contribution);
    when(recordContributor.isContributed(any(), any(Instance.class))).thenReturn(false);

    jobRunner.runInstanceContribution(CENTRAL_SERVER_ID, instance);

    verify(recordContributor).contributeInstance(any(), any());
    verify(recordContributor).contributeItems(any(), any(), anyList());
  }

  @Test
  void runInstanceContribution_shouldDeContribute() {
    var instance = createInstance();
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(false);
    when(contributionService.createOngoingContribution(any())).thenReturn(contribution);
    when(recordContributor.isContributed(any(), any(Instance.class))).thenReturn(true);

    jobRunner.runInstanceContribution(CENTRAL_SERVER_ID, instance);

    verify(recordContributor).deContributeInstance(any(), any());
  }

  @Test
  void runInstanceDeContribution() {
    var instance = createInstance();
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(contributionService.createOngoingContribution(any())).thenReturn(contribution);
    when(recordContributor.isContributed(any(), any(Instance.class))).thenReturn(true);

    jobRunner.runInstanceDeContribution(CENTRAL_SERVER_ID, instance);

    verify(recordContributor).deContributeInstance(any(), any());
  }

  @Test
  void runInstanceDeContribution_shouldSkipNonContributed() {
    var instance = createInstance();
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(recordContributor.isContributed(any(), any(Instance.class))).thenReturn(false);

    jobRunner.runInstanceDeContribution(CENTRAL_SERVER_ID, instance);

    verify(contributionService, never()).createOngoingContribution(any());
    verify(recordContributor, never()).deContributeInstance(any(), any());
  }

  @Test
  void runItemContribution() {
    var item = createItem();
    var instance = new Instance().source(MARC_RECORD_SOURCE);
    instance.addItemsItem(item);
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Item.class))).thenReturn(true);
    when(contributionService.createOngoingContribution(any())).thenReturn(contribution);
    when(recordContributor.isContributed(any(), any(), any(Item.class))).thenReturn(true);

    jobRunner.runItemContribution(CENTRAL_SERVER_ID, instance, item);

    verify(recordContributor).contributeItems(any(), any(), anyList());
    verify(recordContributor).contributeInstance(any(), any());
  }

  @Test
  void runItemContribution_shouldDeContributeIneligible() {
    var item = createItem();
    var instance = new Instance().source(MARC_RECORD_SOURCE);
    instance.addItemsItem(item);
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Item.class))).thenReturn(false);
    when(contributionService.createOngoingContribution(any())).thenReturn(contribution);
    when(recordContributor.isContributed(any(), any(), any(Item.class))).thenReturn(true);

    jobRunner.runItemContribution(CENTRAL_SERVER_ID, instance, item);

    verify(recordContributor).deContributeItem(any(), any());
    verify(recordContributor).contributeInstance(any(), any());
  }

  @Test
  void runItemContribution_shouldSkipIneligible() {
    var instance = createInstance();
    var item = instance.getItems().get(0);
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(validationService.isEligibleForContribution(any(), any(Item.class))).thenReturn(false);
    when(recordContributor.isContributed(any(), any(), any(Item.class))).thenReturn(false);

    jobRunner.runItemContribution(CENTRAL_SERVER_ID, instance, item);

    verify(recordContributor, never()).contributeItems(any(), any(), anyList());
    verify(contributionService, never()).createOngoingContribution(any());
  }

  @Test
  void runItemMove() {
    var oldInstance = createInstance();
    var newInstance = createInstance();
    var item = newInstance.getItems().get(0);
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Item.class))).thenReturn(true);
    when(contributionService.createOngoingContribution(any())).thenReturn(contribution);
    when(recordContributor.isContributed(any(), any(), any(Item.class))).thenReturn(true);

    jobRunner.runItemMove(CENTRAL_SERVER_ID, oldInstance, newInstance, item);

    verify(recordContributor).deContributeItem(any(), any());
    verify(recordContributor).contributeInstance(any(), eq(oldInstance));
    verify(recordContributor).contributeInstance(any(), eq(newInstance));
    verify(recordContributor).contributeItems(any(), any(), anyList());
  }

  @Test
  void runItemDeContribution_shouldDeContribute() {
    var item = createItem();
    var instance = new Instance().source(MARC_RECORD_SOURCE);
    instance.addItemsItem(item);
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(true);
    when(contributionService.createOngoingContribution(any())).thenReturn(contribution);
    when(recordContributor.isContributed(any(), any(), any(Item.class))).thenReturn(true);

    jobRunner.runItemDeContribution(CENTRAL_SERVER_ID, instance, item);

    verify(recordContributor).deContributeItem(any(), any());
    verify(recordContributor).contributeInstance(any(), any());
  }

  @Test
  void runItemDeContribution_shouldSkipNonContributed() {
    var item = createItem();
    var instance = new Instance().source(MARC_RECORD_SOURCE);
    instance.addItemsItem(item);
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(recordContributor.isContributed(any(), any(), any())).thenReturn(false);

    jobRunner.runItemDeContribution(CENTRAL_SERVER_ID, instance, item);

    verify(recordContributor, never()).deContributeItem(any(), any());
    verify(recordContributor, never()).contributeInstance(any(), any());
  }

  @Test
  void runItemDeContribution_shouldDeContributeInstance() {
    var item = createItem();
    var instance = new Instance().source(MARC_RECORD_SOURCE);
    instance.addItemsItem(item);
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(false);
    when(contributionService.createOngoingContribution(any())).thenReturn(contribution);
    when(recordContributor.isContributed(any(), any(), any())).thenReturn(true);

    jobRunner.runItemDeContribution(CENTRAL_SERVER_ID, instance, item);

    verify(recordContributor).deContributeInstance(any(), any());
  }

  @Test
  @Disabled
  void shouldCancelJob_afterOneEvent() throws ExecutionException, InterruptedException, TimeoutException {
    var event = InstanceIterationEvent.of(ITERATION_JOB_ID, "test", "test", UUID.randomUUID());

    when(factory.createReader(any())).thenReturn(reader);
    when(reader.read())
      .thenAnswer(a -> {
        jobRunner.cancelInitialContribution(CONTRIBUTION_ID);
        return event;
      })
      .thenReturn(event);

    jobRunner.startInitialContribution(
      CENTRAL_SERVER_ID, TENANT_ID, CONTRIBUTION_ID, ITERATION_JOB_ID, Integer.valueOf(100));

    verify(reader, times(1)).read();
    verify(contributionService, never()).completeContribution(CONTRIBUTION_ID);
  }

}
