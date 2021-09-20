package org.folio.innreach.batch.contribution.listener;

import java.util.List;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.dto.Item;

@StepScope
@Component
public class ItemExceptionListener extends ContributionExceptionListener<Item, Item> {

  public ItemExceptionListener(ContributionService contributionService, ContributionJobContext context) {
    super(contributionService, context);
  }

  @Override
  public void onReadError(Exception e) {
    logReaderError(e);
  }

  @Override
  public void onProcessError(Item item, Exception e) {
    logProcessError(e, item.getId());
  }

  @Override
  public void onWriteError(Exception e, List<? extends Item> records) {
    var recordId = records.size() == 1 ? records.get(0).getId() : null;
    logWriteError(e, recordId);
  }

}
