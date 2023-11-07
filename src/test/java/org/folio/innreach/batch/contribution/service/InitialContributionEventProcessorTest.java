package org.folio.innreach.batch.contribution.service;

import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.domain.entity.JobExecutionStatus;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.domain.service.RecordContributionService;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.repository.CentralServerRepository;
import org.folio.innreach.repository.ContributionErrorRepository;
import org.folio.innreach.repository.ContributionRepository;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql", "classpath:db/contribution-criteria/clear-contribution-criteria-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@Sql(scripts = {
  "classpath:db/central-server/pre-populate-central-server.sql", "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql",
},
  executionPhase = BEFORE_TEST_METHOD)
@SqlMergeMode(MERGE)
class InitialContributionEventProcessorTest extends BaseControllerTest {
  private final UUID CENTRAL_SERVER_ID = UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");
  private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(15);
  @Autowired
  InitialContributionEventProcessor eventProcessor;
  @Autowired
  ContributionRepository contributionRepository;
  @SpyBean
  JobExecutionStatusRepository jobExecutionStatusRepository;
  @Autowired
  CentralServerRepository centralServerRepository;
  @MockBean
  InventoryViewService inventoryViewService;
  @MockBean
  RecordContributionService recordContributionService;
  @MockBean
  ContributionValidationService validationService;
  @SpyBean
  @Qualifier("instanceExceptionListener")
  ContributionExceptionListener instanceExceptionListener;
  @SpyBean
  @Qualifier("itemExceptionListener")
  ContributionExceptionListener itemExceptionListener;

  @Autowired
  ContributionErrorRepository contributionErrorRepository;

  @Test
  void testInvalidInstanceId() {
    UUID jobId = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();
    JobExecutionStatus job = jobExecutionStatusRepository.save(createMockJobExecution(jobId, instanceId, false));
    contributionRepository.save(createMockContribution(jobId));
    when(inventoryViewService.getInstance(any())).thenReturn(null);
    eventProcessor.processInitialContributionEvents(job);
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() -> verify(jobExecutionStatusRepository, times(2)).save(job));
    var jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId()).get();
    assertEquals(JobExecutionStatus.Status.FAILED, jobExecutionStatus.getStatus());
    assertFalse(jobExecutionStatus.isInstanceContributed());
  }

  @Test
  void testInvalidCentralServerId() {
    UUID jobId = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();
    JobExecutionStatus job = jobExecutionStatusRepository.save(createMockJobExecution(jobId, instanceId, false));
    contributionRepository.save(createMockContribution(UUID.randomUUID()));
    when(inventoryViewService.getInstance(any())).thenReturn(null);
    eventProcessor.processInitialContributionEvents(job);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() -> verify(jobExecutionStatusRepository, times(2)).save(job));
    var jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId()).get();
    assertEquals(JobExecutionStatus.Status.FAILED, jobExecutionStatus.getStatus());
    assertFalse(jobExecutionStatus.isInstanceContributed());
  }

  @Test
  void testInstanceWithInvalidProperties() {
    UUID jobId = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();
    JobExecutionStatus job = jobExecutionStatusRepository.save(createMockJobExecution(jobId, instanceId, false));
    contributionRepository.save(createMockContribution(jobId));
    var instance = createMockInstance(instanceId);
    instance.setSource("MARC!");
    when(inventoryViewService.getInstance(instanceId)).thenReturn(instance);
    when(validationService.isEligibleForContribution(CENTRAL_SERVER_ID, instance)).thenReturn(false);
    eventProcessor.processInitialContributionEvents(job);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() -> verify(jobExecutionStatusRepository, times(2)).save(job));
    var jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId()).get();
    assertEquals(JobExecutionStatus.Status.FAILED, jobExecutionStatus.getStatus());
    assertFalse(jobExecutionStatus.isInstanceContributed());
  }

  @Test
  void testValidInstance() {
    UUID jobId = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();
    JobExecutionStatus job = jobExecutionStatusRepository.save(createMockJobExecution(jobId, instanceId, false));
    contributionRepository.save(createMockContribution(jobId));
    var instance = createMockInstance(instanceId);
    var item = instance.getItems();
    var effectiveLocationId = UUID.randomUUID();
    item.get(0).setEffectiveLocationId(effectiveLocationId);
    item.get(0).setStatisticalCodeIds(Set.of());
    when(validationService.isEligibleForContribution(CENTRAL_SERVER_ID, instance)).thenReturn(true);
    when(inventoryViewService.getInstance(instanceId)).thenReturn(instance);
    doNothing().when(recordContributionService).contributeInstanceWithoutRetry(any(), any());
    eventProcessor.processInitialContributionEvents(job);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() -> verify(jobExecutionStatusRepository, times(2)).save(job));
    var jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId()).get();
    assertEquals(JobExecutionStatus.Status.READY, jobExecutionStatus.getStatus());
    assertTrue(jobExecutionStatus.isInstanceContributed());
  }

  @Test
  void testValidItemWithInstanceAlreadyContributed() {
    UUID jobId = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();
    JobExecutionStatus job = createMockJobExecution(jobId, instanceId, true);
    job.setInstanceContributed(true);
    jobExecutionStatusRepository.save(job);
    contributionRepository.save(createMockContribution(jobId));
    var instance = createMockInstance(instanceId);
    when(validationService.isEligibleForContribution(CENTRAL_SERVER_ID, instance)).thenReturn(true);
    when(inventoryViewService.getInstance(instanceId)).thenReturn(instance);
    when(validationService.isEligibleForContribution(CENTRAL_SERVER_ID, instance.getItems().get(0))).thenReturn(true);
    doNothing().when(recordContributionService).contributeItemsWithoutRetry(any(), any(), any());
    eventProcessor.processInitialContributionEvents(job);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() -> verify(jobExecutionStatusRepository, times(2)).save(job));
    var jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId());
    assertEquals(JobExecutionStatus.Status.PROCESSED, jobExecutionStatus.get().getStatus());
  }

  @Test
  void testInstanceContributionThrowsRetryException() {
    UUID jobId = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();
    JobExecutionStatus job = jobExecutionStatusRepository.save(createMockJobExecution(jobId, instanceId, false));
    contributionRepository.save(createMockContribution(jobId));
    var instance = createMockInstance(instanceId);
    when(validationService.isEligibleForContribution(CENTRAL_SERVER_ID, instance)).thenReturn(true);
    when(inventoryViewService.getInstance(instanceId)).thenReturn(instance);
    when(validationService.isEligibleForContribution(CENTRAL_SERVER_ID, instance.getItems().get(0))).thenReturn(true);
    doThrow(new ServiceSuspendedException("Service suspended")).when(recordContributionService).contributeInstanceWithoutRetry(any(), any());
    eventProcessor.processInitialContributionEvents(job);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() -> verify(jobExecutionStatusRepository, times(2)).save(job));
    var jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId()).get();
    assertEquals(JobExecutionStatus.Status.RETRY, jobExecutionStatus.getStatus());
    assertFalse(jobExecutionStatus.isInstanceContributed());
    assertEquals(1, jobExecutionStatus.getRetryAttempts());

    doThrow(new InnReachConnectionException("Inn reach connection exception")).when(recordContributionService).contributeInstanceWithoutRetry(any(), any());
    eventProcessor.processInitialContributionEvents(jobExecutionStatusRepository.findById(job.getId()).get());

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() -> verify(jobExecutionStatusRepository, times(3)).save(job));
    jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId()).get();
    assertEquals(JobExecutionStatus.Status.RETRY, jobExecutionStatus.getStatus());
    assertFalse(jobExecutionStatus.isInstanceContributed());
    assertEquals(2, jobExecutionStatus.getRetryAttempts());
  }

  @Test
  void testItemContributionThrowsRetryException() {
    UUID jobId = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();
    JobExecutionStatus job = createMockJobExecution(jobId, instanceId, true);
    job.setInstanceContributed(true);
    jobExecutionStatusRepository.save(job);
    contributionRepository.save(createMockContribution(jobId));
    var instance = createMockInstance(instanceId);
    when(validationService.isEligibleForContribution(CENTRAL_SERVER_ID, instance)).thenReturn(true);
    when(inventoryViewService.getInstance(instanceId)).thenReturn(instance);
    when(validationService.isEligibleForContribution(CENTRAL_SERVER_ID, instance.getItems().get(0))).thenReturn(true);
    doThrow(new ServiceSuspendedException("Service suspended")).when(recordContributionService).contributeItemsWithoutRetry(any(), any(), any());
    eventProcessor.processInitialContributionEvents(job);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() -> verify(jobExecutionStatusRepository, times(2)).save(job));
    var jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId()).get();
    assertEquals(JobExecutionStatus.Status.RETRY, jobExecutionStatus.getStatus());
    assertTrue(jobExecutionStatus.isInstanceContributed());
    assertEquals(1, jobExecutionStatus.getRetryAttempts());

    doThrow(new InnReachConnectionException("Inn reach connection exception")).when(recordContributionService).contributeItemsWithoutRetry(any(), any(), any());
    eventProcessor.processInitialContributionEvents(jobExecutionStatusRepository.findById(job.getId()).get());

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() -> verify(jobExecutionStatusRepository, times(3)).save(job));
    jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId()).get();
    assertEquals(JobExecutionStatus.Status.RETRY, jobExecutionStatus.getStatus());
    assertTrue(jobExecutionStatus.isInstanceContributed());
    assertEquals(2, jobExecutionStatus.getRetryAttempts());
  }

  @Test
  void testMaximumRetriesThrowsException() {
    UUID jobId = UUID.randomUUID();
    UUID instanceId1 = UUID.randomUUID();
    var job1 = createMockJobExecution(jobId, instanceId1, false);
    job1.setRetryAttempts(11);
    jobExecutionStatusRepository.save(job1);
    contributionRepository.save(createMockContribution(jobId));
    var instance = createMockInstance(instanceId1);
    when(inventoryViewService.getInstance(instanceId1)).thenReturn(instance);
    eventProcessor.processInitialContributionEvents(job1);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() -> {
      verify(instanceExceptionListener, times(1)).logError(any(), any(), any());
      verify(jobExecutionStatusRepository, times(2)).save(job1);
    });
    var jobExecutionStatus = jobExecutionStatusRepository.findById(job1.getId()).get();
    assertEquals(JobExecutionStatus.Status.FAILED, jobExecutionStatus.getStatus());
    assertEquals(11, jobExecutionStatus.getRetryAttempts());
    assertEquals(1, contributionErrorRepository.findAll().stream().filter(err -> err.getRecordId().equals(instanceId1))
      .count());


    jobId = UUID.randomUUID();
    var instanceId2 = UUID.randomUUID();
    var job2 = createMockJobExecution(jobId, instanceId2, true);
    job2.setRetryAttempts(11);
    jobExecutionStatusRepository.save(job2);
    contributionRepository.save(createMockContribution(jobId));
    instance = createMockInstance(instanceId2);
    when(inventoryViewService.getInstance(instanceId2)).thenReturn(instance);
    eventProcessor.processInitialContributionEvents(job2);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() -> {
      verify(jobExecutionStatusRepository, times(2)).save(job2);
      verify(itemExceptionListener, times(1)).logError(any(), any(), any());
    });
    jobExecutionStatus = jobExecutionStatusRepository.findById(job2.getId()).get();
    assertEquals(JobExecutionStatus.Status.FAILED, jobExecutionStatus.getStatus());
    assertEquals(11, jobExecutionStatus.getRetryAttempts());
    assertEquals(1, contributionErrorRepository.findAll().stream().filter(err -> err.getRecordId().equals(instanceId2))
      .count());
  }

  @Test
  void testBothInstanceAndItemContribution() {
    UUID jobId = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();
    JobExecutionStatus job = jobExecutionStatusRepository.save(createMockJobExecution(jobId, instanceId, false));
    contributionRepository.save(createMockContribution(jobId));
    var instance = createMockInstance(instanceId);
    var item = instance.getItems();
    var effectiveLocationId = UUID.randomUUID();
    item.get(0).setEffectiveLocationId(effectiveLocationId);
    item.get(0).setStatisticalCodeIds(Set.of());
    when(validationService.isEligibleForContribution(CENTRAL_SERVER_ID, instance)).thenReturn(true);
    when(inventoryViewService.getInstance(instanceId)).thenReturn(instance);
    doNothing().when(recordContributionService).contributeInstanceWithoutRetry(any(), any());
    eventProcessor.processInitialContributionEvents(job);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() -> verify(jobExecutionStatusRepository, times(2)).save(job));
    var jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId());
    assertEquals(JobExecutionStatus.Status.READY, jobExecutionStatus.get().getStatus());
    assertTrue(jobExecutionStatus.get().isInstanceContributed());

    when(validationService.isEligibleForContribution(CENTRAL_SERVER_ID, instance.getItems().get(0))).thenReturn(true);
    doNothing().when(recordContributionService).contributeItemsWithoutRetry(any(), any(), any());
    eventProcessor.processInitialContributionEvents(job);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() -> verify(jobExecutionStatusRepository, times(3)).save(job));
    jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId());
    assertEquals(JobExecutionStatus.Status.PROCESSED, jobExecutionStatus.get().getStatus());
    assertTrue(jobExecutionStatus.get().isInstanceContributed());
  }

  @Test
  void testDeContributeInstance() throws SocketTimeoutException {
    UUID jobId = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();
    JobExecutionStatus job = jobExecutionStatusRepository.save(createMockJobExecution(jobId, instanceId, false));
    contributionRepository.save(createMockContribution(jobId));
    var instance = createMockInstance(instanceId);
    var item = instance.getItems();
    var effectiveLocationId = UUID.randomUUID();
    item.get(0).setEffectiveLocationId(effectiveLocationId);
    item.get(0).setStatisticalCodeIds(Set.of());
    when(validationService.isEligibleForContribution(CENTRAL_SERVER_ID, instance)).thenReturn(false);
    when(recordContributionService.isContributed(CENTRAL_SERVER_ID, instance)).thenReturn(true);
    when(inventoryViewService.getInstance(instanceId)).thenReturn(instance);
    doNothing().when(recordContributionService).deContributeInstance(any(), any());
    eventProcessor.processInitialContributionEvents(job);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() -> verify(jobExecutionStatusRepository, times(2)).save(job));
    var jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId());
    assertEquals(JobExecutionStatus.Status.DE_CONTRIBUTED, jobExecutionStatus.get().getStatus());
  }


  private Instance createMockInstance(UUID instanceId) {
    var instance = new Instance();
    instance.setSource("MARC");
    instance.setId(instanceId);
    instance.setHrid("HRID");
    instance.setStatisticalCodeIds(Set.of());
    instance.setItems(List.of(createMockItem()));
    return instance;
  }

  private Item createMockItem() {
    var item = new Item();
    item.setId(UUID.randomUUID());
    item.setBarcode("item");
    return item;
  }

  private Contribution createMockContribution(UUID jobId) {
    Contribution contribution = new Contribution();
    contribution.setId(UUID.randomUUID());
    contribution.setCentralServer(centralServerRepository.findById(CENTRAL_SERVER_ID).get());
    contribution.setStatus(Contribution.Status.IN_PROGRESS);
    contribution.setRecordsTotal(1L);
    contribution.setRecordsProcessed(0L);
    contribution.setRecordsContributed(0L);
    contribution.setRecordsUpdated(0L);
    contribution.setRecordsDecontributed(0L);
    contribution.setJobId(jobId);
    return contribution;
  }

  private JobExecutionStatus createMockJobExecution(UUID jobId, UUID instanceId, boolean isInstanceContributed) {
    JobExecutionStatus jobExecutionStatus = new JobExecutionStatus();
    jobExecutionStatus.setJobId(jobId);
    jobExecutionStatus.setStatus(JobExecutionStatus.Status.IN_PROGRESS);
    jobExecutionStatus.setType("ITERATE");
    jobExecutionStatus.setTenant("test");
    jobExecutionStatus.setInstanceId(instanceId);
    jobExecutionStatus.setInstanceContributed(isInstanceContributed);
    return jobExecutionStatus;
  }
}
