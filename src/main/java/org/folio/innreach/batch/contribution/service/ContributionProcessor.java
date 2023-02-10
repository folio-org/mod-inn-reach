package org.folio.innreach.batch.contribution.service;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;

@Log4j2
@AllArgsConstructor
public class ContributionProcessor implements IMessageProcessor{

  private ContributionJobRunner contributionJobRunner;

  @Override
  public void processMessage(String key, InstanceIterationEvent event, ContributionJobContext context) {
    System.out.println("Message is:"+ event.toString());
//    contributionJobRunner.runInitialContribution(context);
    contributionJobRunner.simulateContribution();
  }
}
