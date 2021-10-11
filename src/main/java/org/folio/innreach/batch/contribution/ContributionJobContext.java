package org.folio.innreach.batch.contribution;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ContributionJobContext {

  public static final String TENANT_ID_KEY = "tenantId";
  public static final String CENTRAL_SERVER_ID_KEY = "centralServerId";
  public static final String CONTRIBUTION_ID_KEY = "contributionId";
  public static final String ITERATION_JOB_ID_KEY = "iterationJobId";

  private String tenantId;
  private UUID centralServerId;
  private UUID contributionId;
  private UUID iterationJobId;

  @Data
  public static class Statistics {
    public int readCount = 0;
    public int writeCount = 0;
    public int writeSkipCount = 0;
  }

}
