package org.folio.innreach.batch.contribution.service;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;

public interface IMessageProcessor {

  public void processMessage(InstanceIterationEvent instanceIterationEvent, ContributionJobContext.Statistics statistics, String topic);
}
