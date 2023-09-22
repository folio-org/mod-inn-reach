package org.folio.innreach.ccheduler;

import lombok.extern.log4j.Log4j2;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@Log4j2
@Lazy
public class SchedulerConfig {
  @Autowired
  private JobExecutionStatusRepository jobExecutionStatusRepository;

  @PostConstruct
  public void initialize(){
    log.info("InitialContributionJobScheduler:: initialize");
    try{
      jobExecutionStatusRepository.updateJobExecutionRecordsByStatus();
    }
    catch (Exception ex){
      log.info("exception ",ex);
    }
  }
}
