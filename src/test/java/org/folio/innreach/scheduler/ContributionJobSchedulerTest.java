package org.folio.innreach.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.common.cache.Cache;
import org.folio.innreach.batch.contribution.service.InitialContributionEventProcessor;
import org.folio.innreach.batch.contribution.service.OngoingContributionEventProcessor;
import org.folio.innreach.domain.entity.JobExecutionStatus;
import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.folio.innreach.domain.entity.TenantInfo;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.folio.innreach.repository.OngoingContributionStatusRepository;
import org.folio.innreach.repository.TenantInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContributionJobSchedulerTest {

  private static final String TENANT_ID = "test-tenant";
  private static final int RECORD_LIMIT = 10;
  private static final double ITEM_PAUSE = 0.0;

  @Mock
  private TenantScopedExecutionService tenantScopedExecutionService;
  @Mock
  private JobExecutionStatusRepository jobExecutionStatusRepository;
  @Mock
  private InitialContributionEventProcessor eventProcessor;
  @Mock
  private TenantInfoRepository tenantRepository;
  @Mock
  private ContributionService contributionService;
  @Mock
  private OngoingContributionStatusRepository ongoingContributionStatusRepository;
  @Mock
  private OngoingContributionEventProcessor ongoingContributionEventProcessor;
  @Mock
  private Cache<String, List<String>> tenantDetailsCache;

  @InjectMocks
  private ContributionJobScheduler scheduler;

  @BeforeEach
  void setUp() throws Exception {
    setField("recordLimit", RECORD_LIMIT);
    setField("itemPause", ITEM_PAUSE);

    doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(1);
      runnable.run();
      return null;
    }).when(tenantScopedExecutionService).runTenantScoped(any(), any());
  }

  @Test
  void postConstruct_updatesInProgressRecordsToReadyForEachTenant() {
    when(tenantDetailsCache.getIfPresent(any())).thenReturn(List.of(TENANT_ID));

    scheduler.postConstruct();

    verify(jobExecutionStatusRepository).updateInProgressRecordsToReady();
    verify(ongoingContributionStatusRepository).updateInProgressToReady();
  }

  @Test
  void processInitialContributionEvents_processesRecordsWhenBelowLimit() {
    var job = new JobExecutionStatus();
    when(tenantDetailsCache.getIfPresent(any())).thenReturn(List.of(TENANT_ID));
    when(jobExecutionStatusRepository.getInProgressRecordsCount()).thenReturn(0L);
    when(jobExecutionStatusRepository.updateAndFetchJobExecutionRecordsByStatus(anyInt(), anyDouble()))
      .thenReturn(List.of(job));

    scheduler.processInitialContributionEvents();

    verify(eventProcessor).processInitialContributionEvents(job);
    verify(contributionService).updateStatisticsAndContributionStatus();
  }

  @Test
  void processInitialContributionEvents_skipsProcessingWhenInProgressCountExceedsLimit() {
    when(tenantDetailsCache.getIfPresent(any())).thenReturn(List.of(TENANT_ID));
    when(jobExecutionStatusRepository.getInProgressRecordsCount()).thenReturn((long) RECORD_LIMIT + 1);

    scheduler.processInitialContributionEvents();

    verify(jobExecutionStatusRepository, never()).updateAndFetchJobExecutionRecordsByStatus(anyInt(), anyDouble());
    verify(eventProcessor, never()).processInitialContributionEvents(any());
  }

  @Test
  void processInitialContributionEvents_doesNotCallProcessorWhenNoNewRecords() {
    when(tenantDetailsCache.getIfPresent(any())).thenReturn(List.of(TENANT_ID));
    when(jobExecutionStatusRepository.getInProgressRecordsCount()).thenReturn(0L);
    when(jobExecutionStatusRepository.updateAndFetchJobExecutionRecordsByStatus(anyInt(), anyDouble()))
      .thenReturn(Collections.emptyList());

    scheduler.processInitialContributionEvents();

    verify(eventProcessor, never()).processInitialContributionEvents(any());
    verify(contributionService).updateStatisticsAndContributionStatus();
  }

  @Test
  void processInitialContributionEvents_processesEachRecordForMultipleTenants() {
    var job1 = new JobExecutionStatus();
    var job2 = new JobExecutionStatus();
    job1.setId(UUID.randomUUID());
    job2.setId(UUID.randomUUID());
    when(tenantDetailsCache.getIfPresent(any())).thenReturn(List.of(TENANT_ID, "other-tenant"));
    when(jobExecutionStatusRepository.getInProgressRecordsCount()).thenReturn(0L);
    when(jobExecutionStatusRepository.updateAndFetchJobExecutionRecordsByStatus(anyInt(), anyDouble()))
      .thenReturn(List.of(job1))
      .thenReturn(List.of(job2));

    scheduler.processInitialContributionEvents();

    verify(eventProcessor).processInitialContributionEvents(job1);
    verify(eventProcessor).processInitialContributionEvents(job2);
    verify(contributionService, times(2)).updateStatisticsAndContributionStatus();
  }

  @Test
  void processInitialContributionEvents_continuesWithOtherTenantsWhenOneFails() {
    var job = new JobExecutionStatus();
    when(tenantDetailsCache.getIfPresent(any())).thenReturn(List.of(TENANT_ID, "other-tenant"));
    when(jobExecutionStatusRepository.getInProgressRecordsCount())
      .thenThrow(new RuntimeException("DB error"))
      .thenReturn(0L);
    when(jobExecutionStatusRepository.updateAndFetchJobExecutionRecordsByStatus(anyInt(), anyDouble()))
      .thenReturn(List.of(job));

    scheduler.processInitialContributionEvents();

    verify(eventProcessor).processInitialContributionEvents(job);
  }

  @Test
  void processOngoingContributionEvents_processesRecordsWhenBelowLimit() {
    var ongoingJob = new OngoingContributionStatus();
    when(tenantDetailsCache.getIfPresent(any())).thenReturn(List.of(TENANT_ID));
    when(ongoingContributionStatusRepository.getInProgressRecordsCount()).thenReturn(0L);
    when(ongoingContributionStatusRepository.updateAndFetchOngoingContributionRecordsByStatus(anyInt()))
      .thenReturn(List.of(ongoingJob));

    scheduler.processOngoingContributionEvents();

    verify(ongoingContributionEventProcessor).processOngoingContribution(ongoingJob);
  }

  @Test
  void processOngoingContributionEvents_skipsProcessingWhenInProgressCountExceedsLimit() {
    when(tenantDetailsCache.getIfPresent(any())).thenReturn(List.of(TENANT_ID));
    when(ongoingContributionStatusRepository.getInProgressRecordsCount()).thenReturn((long) RECORD_LIMIT + 1);

    scheduler.processOngoingContributionEvents();

    verify(ongoingContributionStatusRepository, never()).updateAndFetchOngoingContributionRecordsByStatus(anyInt());
    verify(ongoingContributionEventProcessor, never()).processOngoingContribution(any());
  }

  @Test
  void processOngoingContributionEvents_doesNotCallProcessorWhenNoNewRecords() {
    when(tenantDetailsCache.getIfPresent(any())).thenReturn(List.of(TENANT_ID));
    when(ongoingContributionStatusRepository.getInProgressRecordsCount()).thenReturn(0L);
    when(ongoingContributionStatusRepository.updateAndFetchOngoingContributionRecordsByStatus(anyInt()))
      .thenReturn(Collections.emptyList());

    scheduler.processOngoingContributionEvents();

    verify(ongoingContributionEventProcessor, never()).processOngoingContribution(any());
  }

  @Test
  void processOngoingContributionEvents_continuesWithOtherTenantsWhenOneFails() {
    var ongoingJob = new OngoingContributionStatus();
    when(tenantDetailsCache.getIfPresent(any())).thenReturn(List.of(TENANT_ID, "other-tenant"));
    when(ongoingContributionStatusRepository.getInProgressRecordsCount())
      .thenThrow(new RuntimeException("DB error"))
      .thenReturn(0L);
    when(ongoingContributionStatusRepository.updateAndFetchOngoingContributionRecordsByStatus(anyInt()))
      .thenReturn(List.of(ongoingJob));

    scheduler.processOngoingContributionEvents();

    verify(ongoingContributionEventProcessor).processOngoingContribution(ongoingJob);
  }

  @Test
  void loadTenants_returnsCachedTenantsWhenAvailable() {
    when(tenantDetailsCache.getIfPresent("tenantList")).thenReturn(List.of(TENANT_ID));
    when(jobExecutionStatusRepository.getInProgressRecordsCount()).thenReturn(0L);
    when(jobExecutionStatusRepository.updateAndFetchJobExecutionRecordsByStatus(anyInt(), anyDouble()))
      .thenReturn(Collections.emptyList());

    scheduler.processInitialContributionEvents();
    scheduler.processInitialContributionEvents();

    verify(tenantRepository, never()).findAll();
  }

  @Test
  void loadTenants_fetchesFromRepositoryWhenCacheIsEmpty() {
    var tenantInfo = new TenantInfo();
    tenantInfo.setTenantId(TENANT_ID);
    when(tenantDetailsCache.getIfPresent("tenantList")).thenReturn(null);
    when(tenantRepository.findAll()).thenReturn(List.of(tenantInfo));
    when(jobExecutionStatusRepository.getInProgressRecordsCount()).thenReturn(0L);
    when(jobExecutionStatusRepository.updateAndFetchJobExecutionRecordsByStatus(anyInt(), anyDouble()))
      .thenReturn(Collections.emptyList());

    scheduler.processInitialContributionEvents();

    verify(tenantRepository).findAll();
    verify(tenantDetailsCache).put("tenantList", List.of(TENANT_ID));
  }

  @Test
  void loadTenants_deduplicatesTenantIds() {
    var tenantInfo1 = new TenantInfo();
    tenantInfo1.setTenantId(TENANT_ID);
    var tenantInfo2 = new TenantInfo();
    tenantInfo2.setTenantId(TENANT_ID);
    when(tenantDetailsCache.getIfPresent("tenantList")).thenReturn(null);
    when(tenantRepository.findAll()).thenReturn(List.of(tenantInfo1, tenantInfo2));
    when(jobExecutionStatusRepository.getInProgressRecordsCount()).thenReturn(0L);
    when(jobExecutionStatusRepository.updateAndFetchJobExecutionRecordsByStatus(anyInt(), anyDouble()))
      .thenReturn(Collections.emptyList());

    scheduler.processInitialContributionEvents();

    verify(tenantScopedExecutionService, times(1)).runTenantScoped(any(), any());
  }

  private void setField(String fieldName, Object value) throws Exception {
    var field = ContributionJobScheduler.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(scheduler, value);
  }
}

