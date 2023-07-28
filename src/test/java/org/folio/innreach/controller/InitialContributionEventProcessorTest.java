package org.folio.innreach.controller;

import org.folio.innreach.batch.contribution.service.InitialContributionEventProcessor;
import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.domain.entity.JobExecutionStatus;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.domain.service.RecordContributionService;
import org.folio.innreach.external.dto.InnReachResponse;
import org.folio.innreach.external.service.InnReachContributionService;
import org.folio.innreach.repository.CentralServerRepository;
import org.folio.innreach.repository.ContributionRepository;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

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
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql","classpath:db/contribution-criteria/clear-contribution-criteria-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@Sql(scripts = {
  "classpath:db/central-server/pre-populate-central-server.sql","classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql",
},
  executionPhase = BEFORE_TEST_METHOD)
@SqlMergeMode(MERGE)
//@TestPropertySource(properties = {"initial-contribution.scheduler.fixed-delay=100","initial-contribution.scheduler.initial-delay=100"})
class InitialContributionEventProcessorTest extends BaseControllerTest {
  private final UUID CENTRAL_SERVER_ID = UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");
  @Autowired
  InitialContributionEventProcessor eventProcessor;
  @Autowired
  ContributionRepository contributionRepository;
  @Autowired
  JobExecutionStatusRepository jobExecutionStatusRepository;
  @Autowired
  CentralServerRepository centralServerRepository;
  @MockBean
  InventoryViewService inventoryViewService;
  @MockBean
  InnReachContributionService irContributionService;
  @MockBean
  RecordContributionService recordContributionService;
  @MockBean
  ContributionValidationService validationService;

  @Test
  void testInvalidInstanceId() {
    UUID jobId = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();
    JobExecutionStatus job = jobExecutionStatusRepository.save(createMockJobExecution(jobId, instanceId));
    contributionRepository.save(createMockContribution(jobId));
    when(inventoryViewService.getInstance(any())).thenReturn(null);
    eventProcessor.processInitialContributionEvents(job);
    await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      var jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId());
      assertEquals(JobExecutionStatus.Status.FAILED, jobExecutionStatus.get().getStatus());
      assertFalse(jobExecutionStatus.get().isInstanceContributed());
    });
  }

  @Test
  void testInvalidCentralServerId() {
    UUID jobId = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();
    JobExecutionStatus job = jobExecutionStatusRepository.save(createMockJobExecution(jobId, instanceId));
    contributionRepository.save(createMockContribution(UUID.randomUUID()));
    when(inventoryViewService.getInstance(any())).thenReturn(null);
    eventProcessor.processInitialContributionEvents(job);
    await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      var jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId());
      assertEquals(JobExecutionStatus.Status.FAILED, jobExecutionStatus.get().getStatus());
      assertFalse(jobExecutionStatus.get().isInstanceContributed());
    });
  }

  @Test
  void testInstanceWithInvalidProperties() {
    UUID jobId = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();
    JobExecutionStatus job = jobExecutionStatusRepository.save(createMockJobExecution(jobId,instanceId));
    contributionRepository.save(createMockContribution(jobId));
    var instance = createMockInstance(instanceId);
    instance.setSource("MARC!");
    when(inventoryViewService.getInstance(instanceId)).thenReturn(instance);
    when(validationService.isEligibleForContribution(CENTRAL_SERVER_ID, instance)).thenReturn(false);
    when(irContributionService.lookUpBib(any(), any())).thenReturn(InnReachResponse.errorResponse());
    eventProcessor.processInitialContributionEvents(job);
    await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      var jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId());
      assertEquals(JobExecutionStatus.Status.FAILED, jobExecutionStatus.get().getStatus());
      assertFalse(jobExecutionStatus.get().isInstanceContributed());
    });
  }

  @Test
  void testValidInstance() {
      UUID jobId = UUID.randomUUID();
      UUID instanceId = UUID.randomUUID();
      JobExecutionStatus job = jobExecutionStatusRepository.save(createMockJobExecution(jobId, instanceId));
      contributionRepository.save(createMockContribution(jobId));
      var instance = createMockInstance(instanceId);
      var item = instance.getItems();
      var effectiveLocationId = UUID.randomUUID();
      item.get(0).setEffectiveLocationId(effectiveLocationId);
      item.get(0).setStatisticalCodeIds(Set.of());
      when(validationService.isEligibleForContribution(CENTRAL_SERVER_ID, instance)).thenReturn(true);
      when(inventoryViewService.getInstance(instanceId)).thenReturn(instance);
      when(irContributionService.lookUpBib(any(), any())).thenReturn(InnReachResponse.errorResponse());
      doNothing().when(recordContributionService).contributeInstanceWithoutRetry(any(), any());
      eventProcessor.processInitialContributionEvents(job);
      await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
        var jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId());
        assertEquals(JobExecutionStatus.Status.READY, jobExecutionStatus.get().getStatus());
        assertTrue(jobExecutionStatus.get().isInstanceContributed());
      });
  }

  @Test
  void testValidItemWithInstanceAlreadyContributed() {
    UUID jobId = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();
    JobExecutionStatus job = createMockJobExecution(jobId, instanceId);
    job.setInstanceContributed(true);
    jobExecutionStatusRepository.save(job);
    contributionRepository.save(createMockContribution(jobId));
    var instance = createMockInstance(instanceId);
    when(validationService.isEligibleForContribution(CENTRAL_SERVER_ID, instance)).thenReturn(true);
    when(inventoryViewService.getInstance(instanceId)).thenReturn(instance);
    when(validationService.isEligibleForContribution(CENTRAL_SERVER_ID, instance.getItems().get(0))).thenReturn(true);
    doNothing().when(recordContributionService).contributeItemsWithoutRetry(any(), any(), any());
    eventProcessor.processInitialContributionEvents(job);
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      var jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId());
      assertEquals(JobExecutionStatus.Status.PROCESSED, jobExecutionStatus.get().getStatus());
    });
  }

  @Test
  void testBothInstanceAndItemContribution() {
    UUID jobId = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();
    JobExecutionStatus job = jobExecutionStatusRepository.save(createMockJobExecution(jobId, instanceId));
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
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      var jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId());
      assertEquals(JobExecutionStatus.Status.READY, jobExecutionStatus.get().getStatus());
      assertTrue(jobExecutionStatus.get().isInstanceContributed());
    });
    when(validationService.isEligibleForContribution(CENTRAL_SERVER_ID, instance.getItems().get(0))).thenReturn(true);
    doNothing().when(recordContributionService).contributeItemsWithoutRetry(any(), any(), any());
    eventProcessor.processInitialContributionEvents(job);
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      var jobExecutionStatus = jobExecutionStatusRepository.findById(job.getId());
      assertEquals(JobExecutionStatus.Status.PROCESSED, jobExecutionStatus.get().getStatus());
      assertTrue(jobExecutionStatus.get().isInstanceContributed());
    });
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
    contribution.setCentralServer(centralServerRepository.findById(UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2")).get());
    contribution.setStatus(Contribution.Status.IN_PROGRESS);
    contribution.setRecordsTotal(1L);
    contribution.setRecordsProcessed(0L);
    contribution.setRecordsContributed(0L);
    contribution.setRecordsUpdated(0L);
    contribution.setRecordsDecontributed(0L);
    contribution.setJobId(jobId);
    return contribution;
  }

  private JobExecutionStatus createMockJobExecution(UUID jobId,UUID instanceId) {
    JobExecutionStatus jobExecutionStatus = new JobExecutionStatus();
    jobExecutionStatus.setJobId(jobId);
    jobExecutionStatus.setStatus(JobExecutionStatus.Status.IN_PROGRESS);
    jobExecutionStatus.setType("ITERATE");
    jobExecutionStatus.setTenant("test");
    jobExecutionStatus.setInstanceId(instanceId);
    jobExecutionStatus.setInstanceContributed(false);
    return jobExecutionStatus;
  }

}
