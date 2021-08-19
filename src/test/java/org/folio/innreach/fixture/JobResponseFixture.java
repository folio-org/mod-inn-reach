package org.folio.innreach.fixture;

import org.folio.innreach.domain.dto.folio.inventoryStorage.JobResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.folio.innreach.fixture.TestUtil.randomInteger;

public class JobResponseFixture {
  public static JobResponse createJobResponse(){
    var jobResponse = new JobResponse();
    jobResponse.setId(UUID.randomUUID());
    jobResponse.setStatus(JobResponse.JobStatus.IN_PROGRESS);
    jobResponse.setSubmittedDate(OffsetDateTime.now());
    jobResponse.setNumberOfRecordsPublished(randomInteger(100));
    return jobResponse;
  }
}
