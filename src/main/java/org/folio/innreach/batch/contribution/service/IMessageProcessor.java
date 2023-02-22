package org.folio.innreach.batch.contribution.service;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;

public interface IMessageProcessor {

  public void processMessage(String key, InstanceIterationEvent value, ContributionJobContext context, ContributionJobContext.Statistics statistics, String topic);
}
