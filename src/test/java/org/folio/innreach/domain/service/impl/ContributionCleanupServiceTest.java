package org.folio.innreach.domain.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.folio.innreach.repository.OngoingContributionStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContributionCleanupServiceTest {

  @Mock
  private JobExecutionStatusRepository jobRepo;
  @Mock
  private OngoingContributionStatusRepository ongoingRepo;
  @InjectMocks
  private ContributionCleanupServiceImpl service;

  @Test
  void cleanup_whenDisabled_shouldSkip() {
    ReflectionTestUtils.setField(service, "cleanupEnabled", false);

    service.cleanup();

    verifyNoInteractions(jobRepo, ongoingRepo);
  }

  @Test
  void cleanup_whenNoEligibleRecords_shouldDeleteZero() {
    ReflectionTestUtils.setField(service, "cleanupEnabled", true);
    ReflectionTestUtils.setField(service, "retentionDays", 7);
    ReflectionTestUtils.setField(service, "batchSize", 5000);
    when(jobRepo.deleteBatchByStatusAndUpdatedBefore(anyList(), any(OffsetDateTime.class), anyInt()))
      .thenReturn(0);
    when(ongoingRepo.deleteBatchByStatusAndUpdatedBefore(anyList(), any(OffsetDateTime.class), anyInt()))
      .thenReturn(0);

    service.cleanup();

    verify(jobRepo, times(1))
      .deleteBatchByStatusAndUpdatedBefore(anyList(), any(OffsetDateTime.class), anyInt());
    verify(ongoingRepo, times(1))
      .deleteBatchByStatusAndUpdatedBefore(anyList(), any(OffsetDateTime.class), anyInt());
  }

  @Test
  void cleanup_whenRecordsExist_shouldDeleteAllBatches() {
    ReflectionTestUtils.setField(service, "cleanupEnabled", true);
    ReflectionTestUtils.setField(service, "retentionDays", 7);
    ReflectionTestUtils.setField(service, "batchSize", 100);
    when(jobRepo.deleteBatchByStatusAndUpdatedBefore(anyList(), any(OffsetDateTime.class), anyInt()))
      .thenReturn(100)
      .thenReturn(100)
      .thenReturn(50);
    when(ongoingRepo.deleteBatchByStatusAndUpdatedBefore(anyList(), any(OffsetDateTime.class), anyInt()))
      .thenReturn(0);

    service.cleanup();

    verify(jobRepo, times(3))
      .deleteBatchByStatusAndUpdatedBefore(anyList(), any(OffsetDateTime.class), anyInt());
    verify(ongoingRepo, times(1))
      .deleteBatchByStatusAndUpdatedBefore(anyList(), any(OffsetDateTime.class), anyInt());
  }

  @Test
  void cleanup_shouldUseTerminalStatuses() {
    ReflectionTestUtils.setField(service, "cleanupEnabled", true);
    ReflectionTestUtils.setField(service, "retentionDays", 7);
    ReflectionTestUtils.setField(service, "batchSize", 100);
    when(jobRepo.deleteBatchByStatusAndUpdatedBefore(anyList(), any(OffsetDateTime.class), anyInt()))
      .thenReturn(0);
    when(ongoingRepo.deleteBatchByStatusAndUpdatedBefore(anyList(), any(OffsetDateTime.class), anyInt()))
      .thenReturn(0);

    service.cleanup();

    var expectedStatuses = java.util.List.of("PROCESSED", "FAILED", "DE_CONTRIBUTED");
    verify(jobRepo).deleteBatchByStatusAndUpdatedBefore(eq(expectedStatuses), any(OffsetDateTime.class), anyInt());
    verify(ongoingRepo).deleteBatchByStatusAndUpdatedBefore(eq(expectedStatuses), any(OffsetDateTime.class), anyInt());
  }

  @Test
  void cleanup_cutoffShouldUseRetentionDays() {
    ReflectionTestUtils.setField(service, "cleanupEnabled", true);
    ReflectionTestUtils.setField(service, "retentionDays", 10);
    ReflectionTestUtils.setField(service, "batchSize", 100);
    when(jobRepo.deleteBatchByStatusAndUpdatedBefore(anyList(), any(OffsetDateTime.class), anyInt()))
      .thenReturn(0);
    when(ongoingRepo.deleteBatchByStatusAndUpdatedBefore(anyList(), any(OffsetDateTime.class), anyInt()))
      .thenReturn(0);

    service.cleanup();

    var captor = org.mockito.ArgumentCaptor.forClass(OffsetDateTime.class);
    verify(jobRepo).deleteBatchByStatusAndUpdatedBefore(anyList(), captor.capture(), anyInt());
    assertThat(captor.getValue()).isBeforeOrEqualTo(OffsetDateTime.now().minusDays(10));
  }
}
