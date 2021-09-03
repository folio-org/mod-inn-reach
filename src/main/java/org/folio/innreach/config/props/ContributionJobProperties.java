package org.folio.innreach.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("batch.jobs.contribution")
public class ContributionJobProperties {

  private int chunkSize = 1;
  private int retryIntervalMs = 1000;
  private int retryAttempts = 3;
  private String readerTopic;
  private long readerPollTimeoutSec;

}
