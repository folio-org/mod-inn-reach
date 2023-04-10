package org.folio.innreach.batch.contribution.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.folio.innreach.batch.contribution.ContributionJobContextManager.beginContributionJobContext;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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

import java.lang.reflect.Field;
import java.net.SocketTimeoutException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import feign.FeignException;
import org.folio.innreach.batch.contribution.InitialContributionJobConsumerContainer;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.external.exception.SocketTimeOutExceptionWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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

  private static final String TOPIC = "test";
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

  private final ContributionJobContext.Statistics statistics = new ContributionJobContext.Statistics();
  @Spy
  private RetryTemplate retryTemplate = createNoRetryTemplate();

  @InjectMocks
  private ContributionJobRunner jobRunner;

  @Mock
  private InitialContributionJobConsumerContainer initialContributionJobConsumerContainer;

  @Spy
  private ConcurrentHashMap<String, Integer> recordsProcessed = new ConcurrentHashMap<>();

  //added
  @BeforeEach
  void setContext() {
    beginContributionJobContext(JOB_CONTEXT);
  }

  @Test
  void shouldRunJob() throws SocketTimeoutException {
    event = InstanceIterationEvent.of(ITERATION_JOB_ID, "test", "test", UUID.randomUUID());

    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Item.class))).thenReturn(true);
    when(inventoryViewService.getInstance(any())).thenReturn(createInstanceView().toInstance());
    doThrow(new RuntimeException()).when(recordContributor).contributeInstance(any(),any());
    doThrow(new RuntimeException()).when(recordContributor).contributeItems(any(),any(),any());

    jobRunner.runInitialContribution(ContributionJobRunnerTest.this.event,TOPIC);

    verify(recordContributor).contributeInstance(any(), any());
    verify(recordContributor).contributeItems(any(), any(), anyList());
  }

  @Test
  void testContributeItemsException() throws SocketTimeoutException {
    event = InstanceIterationEvent.of(ITERATION_JOB_ID, "test", "test", UUID.randomUUID());

    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Item.class))).thenReturn(true);
    when(inventoryViewService.getInstance(any())).thenReturn(createInstanceView().toInstance());

    doThrow(ServiceSuspendedException.class).when(recordContributor).contributeItems(any(),any(),any());
    assertThatThrownBy(() -> jobRunner.runInitialContribution(ContributionJobRunnerTest.this.event,TOPIC))
      .isInstanceOf(ServiceSuspendedException.class);

    doThrow(FeignException.class).when(recordContributor).contributeItems(any(),any(),any());
    assertThatThrownBy(() -> jobRunner.runInitialContribution(ContributionJobRunnerTest.this.event,TOPIC))
      .isInstanceOf(FeignException.class);

    doThrow(InnReachConnectionException.class).when(recordContributor).contributeItems(any(),any(),any());
    assertThatThrownBy(() -> jobRunner.runInitialContribution(ContributionJobRunnerTest.this.event,TOPIC))
      .isInstanceOf(InnReachConnectionException.class);

    doThrow(SocketTimeoutException.class).when(recordContributor).contributeItems(any(),any(),any());
    assertThatThrownBy(() -> jobRunner.runInitialContribution(ContributionJobRunnerTest.this.event,TOPIC))
      .isInstanceOf(SocketTimeOutExceptionWrapper.class);

  }

  @Test
  void shouldRunJob_noInstanceItems() throws NoSuchFieldException, IllegalAccessException {
    event = InstanceIterationEvent.of(ITERATION_JOB_ID, "test", "test", UUID.randomUUID());
    Instance instance = createInstance();
    instance.setItems(null);

    when(inventoryViewService.getInstance(any())).thenReturn(instance);

    jobRunner.runInitialContribution(ContributionJobRunnerTest.this.event,TOPIC);

    verify(recordContributor).isContributed(CENTRAL_SERVER_ID, instance);

    recordsProcessed.put(JOB_CONTEXT.getTenantId(),10);
    Field field = ContributionJobRunner.class.getDeclaredField("recordsProcessed");
    field.setAccessible(true);
    field.set(null,recordsProcessed);
    jobRunner.runInitialContribution(ContributionJobRunnerTest.this.event,TOPIC);
    Assertions.assertEquals(Integer.valueOf(11), recordsProcessed.get(JOB_CONTEXT.getTenantId()));
    verify(recordContributor,times(2)).isContributed(CENTRAL_SERVER_ID, instance);

  }

  @Test
  void shouldRunJob_deContributeIneligibleInstance() throws SocketTimeoutException {
    event = InstanceIterationEvent.of(ITERATION_JOB_ID, "test", "test", UUID.randomUUID());
    Instance instance = createInstance();
    instance.setItems(null);

    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(recordContributor.isContributed(any(), any())).thenReturn(true);
    doThrow(new RuntimeException()).when(recordContributor).deContributeInstance(any(),any());

    jobRunner.runInitialContribution(ContributionJobRunnerTest.this.event,TOPIC);

    verify(recordContributor).deContributeInstance(CENTRAL_SERVER_ID, instance);
  }

  @Test
  void testDeContributeIneligibleInstanceException() throws SocketTimeoutException {
    event = InstanceIterationEvent.of(ITERATION_JOB_ID, "test", "test", UUID.randomUUID());
    Instance instance = createInstance();
    instance.setItems(null);
    when(inventoryViewService.getInstance(any())).thenReturn(instance);
    when(recordContributor.isContributed(any(), any())).thenReturn(true);
    doThrow(ServiceSuspendedException.class).when(recordContributor).deContributeInstance(any(),any());
    assertThatThrownBy(() -> jobRunner.runInitialContribution(ContributionJobRunnerTest.this.event,TOPIC))
      .isInstanceOf(ServiceSuspendedException.class);

    doThrow(InnReachConnectionException.class).when(recordContributor).deContributeInstance(any(),any());
    assertThatThrownBy(() -> jobRunner.runInitialContribution(ContributionJobRunnerTest.this.event,TOPIC))
      .isInstanceOf(InnReachConnectionException.class);

    doThrow(FeignException.class).when(recordContributor).deContributeInstance(any(),any());
    assertThatThrownBy(() -> jobRunner.runInitialContribution(ContributionJobRunnerTest.this.event,TOPIC))
      .isInstanceOf(FeignException.class);

    doThrow(SocketTimeoutException.class).when(recordContributor).deContributeInstance(any(),any());
    assertThatThrownBy(() -> jobRunner.runInitialContribution(ContributionJobRunnerTest.this.event,TOPIC))
      .isInstanceOf(SocketTimeOutExceptionWrapper.class);
  }

  @Test
  void shouldRunJob_noInstances() {
    event = InstanceIterationEvent.of(ITERATION_JOB_ID, "test", "test", UUID.randomUUID());

    when(inventoryViewService.getInstance(any())).thenReturn(null);

    jobRunner.runInitialContribution(ContributionJobRunnerTest.this.event,TOPIC);

    verify(inventoryViewService).getInstance(any());
    verifyNoMoreInteractions(recordContributor);
  }

  @Test
  void shouldRunJob_noEvents() {

    jobRunner.runInitialContribution(event, TOPIC);

    verifyNoInteractions(recordContributor);
  }

  @Test
  void shouldCancelJobs() {
    jobRunner.cancelJobs();

    verify(contributionService).cancelAll();
  }

  @Test
  void runInstanceContribution_shouldContribute() throws SocketTimeoutException {
    var instance = createInstance();
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Item.class))).thenReturn(true);
    when(contributionService.createOngoingContribution(any())).thenReturn(contribution);
    when(recordContributor.isContributed(any(), any(Instance.class))).thenReturn(false);
    when(folioContext.getTenantId()).thenReturn(JOB_CONTEXT.getTenantId());

    jobRunner.runInstanceContribution(CENTRAL_SERVER_ID, instance);

    verify(recordContributor).contributeInstance(any(), any());
    verify(recordContributor).contributeItems(any(), any(), anyList());
  }

  @Test
  void runInstanceContribution_shouldDeContribute() throws SocketTimeoutException {
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
  void testRunInstanceContribution_IneligibleInstance() throws SocketTimeoutException {
    var instance = createInstance();
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(false);
    when(recordContributor.isContributed(any(), any(Instance.class))).thenReturn(false);
    jobRunner.runInstanceContribution(CENTRAL_SERVER_ID, instance);
    verify(recordContributor,never()).deContributeInstance(any(), any());
  }

  @Test
  void runInstanceDeContribution() throws SocketTimeoutException {
    var instance = createInstance();
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(contributionService.createOngoingContribution(any())).thenReturn(contribution);
    when(recordContributor.isContributed(any(), any(Instance.class))).thenReturn(true);

    jobRunner.runInstanceDeContribution(CENTRAL_SERVER_ID, instance);

    verify(recordContributor).deContributeInstance(any(), any());
  }

  @Test
  void runInstanceDeContribution_shouldSkipNonContributed() throws SocketTimeoutException {
    var instance = createInstance();
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(recordContributor.isContributed(any(), any(Instance.class))).thenReturn(false);

    jobRunner.runInstanceDeContribution(CENTRAL_SERVER_ID, instance);

    verify(contributionService, never()).createOngoingContribution(any());
    verify(recordContributor, never()).deContributeInstance(any(), any());
  }

  @Test
  void runItemContribution() throws SocketTimeoutException {
    var item = createItem();
    var instance = new Instance().source(MARC_RECORD_SOURCE);
    instance.addItemsItem(item);
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Item.class))).thenReturn(true);
    when(contributionService.createOngoingContribution(any())).thenReturn(contribution);
    when(recordContributor.isContributed(any(), any(), any(Item.class))).thenReturn(true);
    when(folioContext.getTenantId()).thenReturn(JOB_CONTEXT.getTenantId());

    jobRunner.runItemContribution(CENTRAL_SERVER_ID, instance, item);

    verify(recordContributor).contributeItems(any(), any(), anyList());
    verify(recordContributor).contributeInstance(any(), any());
  }

  @Test
  void runItemContribution_shouldDeContributeIneligible() throws SocketTimeoutException {
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
  void testDeContributeIneligibleItemException() throws SocketTimeoutException {
    var item = createItem();
    var instance = new Instance().source(MARC_RECORD_SOURCE);
    instance.addItemsItem(item);
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Item.class))).thenReturn(false);
    when(contributionService.createOngoingContribution(any())).thenReturn(contribution);
    when(recordContributor.isContributed(any(), any(), any(Item.class))).thenReturn(true);

    doThrow(RuntimeException.class).when(recordContributor).deContributeItem(any(), any());
    jobRunner.runItemContribution(CENTRAL_SERVER_ID, instance, item);
    verify(recordContributor).deContributeItem(any(), any());
    verify(recordContributor).contributeInstance(any(), any());

    doThrow(ServiceSuspendedException.class).when(recordContributor).deContributeItem(any(), any());
    assertThatThrownBy(() -> jobRunner.runItemContribution(CENTRAL_SERVER_ID, instance, item))
      .isInstanceOf(ServiceSuspendedException.class);

    doThrow(FeignException.class).when(recordContributor).deContributeItem(any(), any());
    assertThatThrownBy(() -> jobRunner.runItemContribution(CENTRAL_SERVER_ID, instance, item))
      .isInstanceOf(FeignException.class);

    doThrow(InnReachConnectionException.class).when(recordContributor).deContributeItem(any(), any());
    assertThatThrownBy(() -> jobRunner.runItemContribution(CENTRAL_SERVER_ID, instance, item))
      .isInstanceOf(InnReachConnectionException.class);
  }

  @Test
  void testContributionInstanceException() throws SocketTimeoutException {
    var item = createItem();
    var instance = new Instance().source(MARC_RECORD_SOURCE);
    instance.addItemsItem(item);
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Item.class))).thenReturn(false);
    when(contributionService.createOngoingContribution(any())).thenReturn(contribution);
    when(recordContributor.isContributed(any(), any(), any(Item.class))).thenReturn(true);

    doThrow(RuntimeException.class).when(recordContributor).contributeInstance(any(), any());
    jobRunner.runItemContribution(CENTRAL_SERVER_ID, instance, item);
    verify(recordContributor).deContributeItem(any(), any());
    verify(recordContributor).contributeInstance(any(), any());

    doThrow(ServiceSuspendedException.class).when(recordContributor).contributeInstance(any(), any());
    assertThatThrownBy(() -> jobRunner.runItemContribution(CENTRAL_SERVER_ID, instance, item))
      .isInstanceOf(ServiceSuspendedException.class);

    doThrow(FeignException.class).when(recordContributor).contributeInstance(any(), any());
    assertThatThrownBy(() -> jobRunner.runItemContribution(CENTRAL_SERVER_ID, instance, item))
      .isInstanceOf(FeignException.class);

    doThrow(InnReachConnectionException.class).when(recordContributor).contributeInstance(any(), any());
    assertThatThrownBy(() -> jobRunner.runItemContribution(CENTRAL_SERVER_ID, instance, item))
      .isInstanceOf(InnReachConnectionException.class);

    doThrow(SocketTimeoutException.class).when(recordContributor).contributeInstance(any(), any());
    assertThatThrownBy(() -> jobRunner.runItemContribution(CENTRAL_SERVER_ID, instance, item))
      .isInstanceOf(SocketTimeOutExceptionWrapper.class);

  }
  @Test
  void runItemContribution_shouldSkipIneligible() throws SocketTimeoutException {
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
  void runItemMove() throws SocketTimeoutException {
    var oldInstance = createInstance();
    var newInstance = createInstance();
    var item = newInstance.getItems().get(0);
    var contribution = new ContributionDTO();
    contribution.setId(UUID.randomUUID());

    when(validationService.isEligibleForContribution(any(), any(Instance.class))).thenReturn(true);
    when(validationService.isEligibleForContribution(any(), any(Item.class))).thenReturn(true);
    when(contributionService.createOngoingContribution(any())).thenReturn(contribution);
    when(recordContributor.isContributed(any(), any(), any(Item.class))).thenReturn(true);
    when(folioContext.getTenantId()).thenReturn(JOB_CONTEXT.getTenantId());

    jobRunner.runItemMove(CENTRAL_SERVER_ID, oldInstance, newInstance, item);

    verify(recordContributor).deContributeItem(any(), any());
    verify(recordContributor).contributeInstance(any(), eq(oldInstance));
    verify(recordContributor).contributeInstance(any(), eq(newInstance));
    verify(recordContributor).contributeItems(any(), any(), anyList());
  }

  @Test
  void runItemDeContribution_shouldDeContribute() throws SocketTimeoutException {
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
  void runItemDeContribution_shouldSkipNonContributed() throws SocketTimeoutException {
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
  void runItemDeContribution_shouldDeContributeInstance() throws SocketTimeoutException {
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
  void startInitialContributionTest() {
    when(factory.createInitialContributionConsumerContainer(any(),any())).thenReturn(initialContributionJobConsumerContainer);
    doNothing().when(initialContributionJobConsumerContainer).tryStartOrCreateConsumer(any());

    jobRunner.startInitialContribution(
      CENTRAL_SERVER_ID, TENANT_ID, CONTRIBUTION_ID, ITERATION_JOB_ID, 100);

    verify(initialContributionJobConsumerContainer,times(1)).tryStartOrCreateConsumer(any());

  }
  @Test
  public void testCancelInitialContribution(){
    jobRunner.cancelInitialContribution(UUID.randomUUID());
    Assertions.assertNotNull(jobRunner);
  }
  @Test
  void shouldRunJob_noEvent() throws SocketTimeoutException {
    jobRunner.runInitialContribution(null,TOPIC);
    verify(recordContributor, never()).deContributeItem(any(), any());
    verify(recordContributor, never()).contributeInstance(any(), any());
  }

  @Test
  void testCancelContributionIfRetryExhausted(){
    UUID uuid = UUID.randomUUID();
    jobRunner.cancelContributionIfRetryExhausted(uuid);
    verify(contributionService).cancelCurrent(uuid);
  }

}
