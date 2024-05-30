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
  @Query(value = "update ongoing_contribution_status set status = 'READY' where status = 'IN_PROGRESS')", nativeQuery = true)
  void updateInProgressToReady();

  @Query(value = """
    update ongoing_contribution_status o1
    set o1.status = 'IN_PROGRESS' where o1.id in (select o2.id from ongoing_contribution_status o2
    where o2.status = 'READY' Order by o2.created_date
    limit :limit) returning *
    """, nativeQuery = true)
  List<OngoingContributionStatus> updateAndFetchOngoingContributionRecordsByStatus(@Param("limit") int limit);

  @Query(value = "select count(*) from ongoing_contribution_status o where o.status='IN_PROGRESS'", nativeQuery = true)
  long getInProgressRecordsCount();
}
