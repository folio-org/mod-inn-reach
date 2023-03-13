package org.folio.innreach.batch.contribution.service;

import org.folio.innreach.batch.contribution.ContributionJobContext.Statistics;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;

public interface IMessageProcessor {

  public void processMessage(InstanceIterationEvent instanceIterationEvent, Statistics statistics, String topic);
}
