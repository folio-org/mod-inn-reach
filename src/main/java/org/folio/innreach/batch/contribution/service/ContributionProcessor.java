package org.folio.innreach.batch.contribution.service;

import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;

public class ContributionProcessor implements IMessageProcessor{
  @Override
  public void processMessage(String key, InstanceIterationEvent value) {
    System.out.println("Message is:"+value.toString());
  }
}
