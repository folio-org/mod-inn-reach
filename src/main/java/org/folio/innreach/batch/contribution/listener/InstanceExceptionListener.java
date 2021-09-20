package org.folio.innreach.batch.contribution.listener;

import java.util.List;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.dto.Instance;

@StepScope
@Component
public class InstanceExceptionListener extends ContributionExceptionListener<InstanceIterationEvent, Instance> {

  public InstanceExceptionListener(ContributionService contributionService, ContributionJobContext context) {
    super(contributionService, context);
  }

  @Override
  public void onReadError(Exception e) {
    logReaderError(e);
  }

  @Override
  public void onProcessError(InstanceIterationEvent event, Exception e) {
    logProcessError(e, event.getInstanceId());
  }

  @Override
  public void onWriteError(Exception e, List<? extends Instance> records) {
    var recordId = records.size() == 1 ? records.get(0).getId() : null;
    logWriteError(e, recordId);
  }

}
