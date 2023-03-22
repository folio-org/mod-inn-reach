package org.folio.innreach.batch.contribution.service;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;


@Log4j2
@AllArgsConstructor
public class ContributionProcessor implements IMessageProcessor{

  private final ContributionJobRunner contributionJobRunner;

  @Override
  public void processMessage(InstanceIterationEvent event, String topic) {
    log.debug("Message is: {}", event.toString());
    contributionJobRunner.runInitialContribution(event, topic);
  }
}
