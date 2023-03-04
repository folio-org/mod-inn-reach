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
    private long recordsTotal;
    private long recordsProcessed;
    private long recordsContributed;
    private long recordsUpdated;
    private long recordsDecontributed;

    private String topic;

    private String tenantId;

    public void addRecordsContributed(int itemsCount) {
      recordsContributed += itemsCount;
    }

    public void addRecordsProcessed(int itemsCount) {
      recordsProcessed += itemsCount;
    }

    public void addRecordsDeContributed(int itemsCount) {
      recordsDecontributed += itemsCount;
    }

    public void addRecordsTotal(int itemsCount) {
      recordsTotal += itemsCount;
    }
  }

}
