package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.JobExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobExecutionStatusRepository extends JpaRepository<JobExecutionStatus, UUID> {

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Transactional
  @Query(value = "WITH claimed AS (" +
    "SELECT t.id FROM job_execution_status t " +
    "JOIN contribution c ON c.job_id = t.job_id " +
    "WHERE c.status = 0 " +
    "AND t.status IN ('READY', 'RETRY') " +
    "AND (t.instance_contributed = false " +
    "OR t.updated_date < current_timestamp - (interval '1 hour') * :itemPause) " +
    "ORDER BY t.instance_contributed ASC, t.updated_date NULLS FIRST " +
    "FOR UPDATE OF t SKIP LOCKED " +
    "LIMIT :limit) " +
    "UPDATE job_execution_status s " +
    "SET status = 'IN_PROGRESS', updated_date = now() " +
    "FROM claimed WHERE s.id = claimed.id " +
    "RETURNING s.*", nativeQuery = true)
  List<JobExecutionStatus> updateAndFetchJobExecutionRecordsByStatus(@Param("limit") int limit, @Param("itemPause") double itemPause);

  @Query(value = "select count(*) from job_execution_status j inner join contribution c on " +
    "c.job_id = j.job_id where c.status = 0 and j.status='IN_PROGRESS'", nativeQuery = true)
  long getInProgressRecordsCount();

  @Modifying
  @Transactional
  @Query(value = "update job_execution_status  set status = 'READY' where id in (select j.id " +
    "from job_execution_status j inner join contribution c on j.job_id = c.job_id " +
    "where c.status=0 and j.status = 'IN_PROGRESS')", nativeQuery = true)
  void updateInProgressRecordsToReady();
}
