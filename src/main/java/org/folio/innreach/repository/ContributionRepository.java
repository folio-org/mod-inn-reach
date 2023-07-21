package org.folio.innreach.repository;

import static org.folio.innreach.domain.entity.Contribution.FETCH_HISTORY_COUNT_QUERY_NAME;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.folio.innreach.domain.entity.Contribution;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, UUID> {

  @Query(name = Contribution.FETCH_CURRENT_QUERY_NAME)
  Optional<Contribution> fetchCurrentByCentralServerId(UUID id);

  @Query(name = Contribution.FETCH_ONGOING_QUERY_NAME)
  Optional<Contribution> fetchOngoingByCentralServerId(UUID id);

  @Query(name = Contribution.FETCH_HISTORY_QUERY_NAME, countName = FETCH_HISTORY_COUNT_QUERY_NAME)
  Page<Contribution> fetchHistoryByCentralServerId(UUID id, Pageable pageable);

  List<Contribution> findAllByStatus(Contribution.Status status);
  Contribution findByJobId(UUID jobId);

  @Modifying
  @Query(value = "Update contribution c set records_total = records_total+1,records_processed = records_processed+1," +
    "records_contributed = records_contributed + :recordsContributed, status = case when (select count(*) from job_execution_status j where j.status in ('READY','RETRY','IN_PROGRESS') " +
    "and j.job_id= :jobId ) = 0 then 1 else status end where c.job_id = :jobId " , nativeQuery = true)
  int updateStatisticsAndStatus(@Param("jobId") UUID jobId, @Param("recordsContributed") int recordsContributed);

}
