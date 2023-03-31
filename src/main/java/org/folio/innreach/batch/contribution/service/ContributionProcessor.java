package org.folio.innreach.batch.contribution.service;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.external.exception.SocketTimeOutExceptionWrapper;


@Log4j2
@AllArgsConstructor
public class ContributionProcessor implements IMessageProcessor{

  private final ContributionJobRunner contributionJobRunner;

  @Override
  public void processMessage(InstanceIterationEvent event, String topic) {
    try {
      log.info("processMessage : Message is: {}", event.toString());
      contributionJobRunner.runInitialContribution(event, topic);
    }
    catch (ServiceSuspendedException | FeignException | InnReachConnectionException | SocketTimeOutExceptionWrapper e) {
      throw e;
    }
    catch (Exception e) {
      log.info("ContributionProcessor: error happened while consuming :{}",e.getMessage());
    }
  }
}
