package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface OngoingContributionStatusRepository extends JpaRepository<OngoingContributionStatus, UUID> {
  @Modifying
  @Transactional
  @Query(value = "update ongoing_contribution_status set status = 'READY' where status = 'IN_PROGRESS'", nativeQuery = true)
  void updateInProgressToReady();

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Transactional
  @Query(value = """
    WITH claimed AS (
      SELECT o.id FROM ongoing_contribution_status o
      WHERE o.status IN ('READY', 'RETRY')
      ORDER BY o.created_date
      FOR UPDATE SKIP LOCKED
      LIMIT :limit)
    UPDATE ongoing_contribution_status s
    SET status = 'IN_PROGRESS', updated_date = now()
    FROM claimed WHERE s.id = claimed.id
    RETURNING s.*
    """, nativeQuery = true)
  List<OngoingContributionStatus> updateAndFetchOngoingContributionRecordsByStatus(@Param("limit") int limit);

  @Query(value = "select count(*) from ongoing_contribution_status o where o.status='IN_PROGRESS'", nativeQuery = true)
  long getInProgressRecordsCount();
}
