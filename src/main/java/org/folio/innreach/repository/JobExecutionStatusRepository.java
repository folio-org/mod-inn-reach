package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.JobExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobExecutionStatusRepository extends JpaRepository<JobExecutionStatus, UUID> {

  @Query(value = "update job_execution_status s " +
    "set status = 'IN_PROGRESS' where s.id in (select t.id from job_execution_status t " +
    "inner join contribution c on c.job_id = t.job_id where c.status = 0 and " +
    "((t.instance_contributed = true and t.updated_date < current_timestamp - interval '0.1 hour') " +
    "or t.instance_contributed = false) and "+
    "t.status in ('READY', 'RETRY') limit :limit) returning * ", nativeQuery = true)
  List<JobExecutionStatus> updateAndFetchJobExecutionRecordsByStatus(@Param("limit") int limit);
}
