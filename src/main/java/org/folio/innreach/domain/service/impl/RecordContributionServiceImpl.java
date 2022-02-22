package org.folio.innreach.domain.service.impl;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.domain.service.RecordContributionService;
import org.folio.innreach.domain.service.RecordTransformationService;
import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.external.dto.BibItem;
import org.folio.innreach.external.dto.BibItemsInfo;
import org.folio.innreach.external.dto.InnReachResponse;
import org.folio.innreach.external.service.InnReachContributionService;

@Log4j2
@Service
@RequiredArgsConstructor
public class RecordContributionServiceImpl implements RecordContributionService {

  @Qualifier("contributionRetryTemplate")
  private final RetryTemplate retryTemplate;
  private final InnReachContributionService irContributionService;
  private final RecordTransformationService recordTransformationService;

  @Qualifier("itemExceptionListener")
  private final ContributionExceptionListener exceptionListener;

  @Override
  public void contributeInstance(UUID centralServerId, Instance instance) {
    var bibId = instance.getHrid();

    log.info("Contributing bib {}", bibId);

    var bib = recordTransformationService.getBibInfo(centralServerId, instance);

    contributeAndVerifyBib(centralServerId, bibId, bib);

    log.info("Finished contribution of bib {}", bibId);
  }

  @Override
  public void deContributeInstance(UUID centralServerId, Instance instance) {
    var bibId = instance.getHrid();
    log.info("De-contributing bib {}", bibId);
    irContributionService.deContributeBib(centralServerId, bibId);
  }

  private void contributeAndVerifyBib(UUID centralServerId, String bibId, BibInfo bib) {
    log.info("Contributing bib {}", bibId);
    retryTemplate.execute(r -> contributeBib(centralServerId, bibId, bib));
    retryTemplate.execute(r -> verifyBibContribution(centralServerId, bibId));
    log.info("Finished contribution of bib {}", bibId);
  }

  @Override
  public boolean isContributed(UUID centralServerId, Instance instance) {
    return irContributionService.lookUpBib(centralServerId, instance.getHrid()).isOk();
  }

  @Override
  public boolean isContributed(UUID centralServerId, Instance instance, Item item) {
    var bibId = instance.getHrid();
    var itemId = item.getHrid();
    return irContributionService.lookUpBibItem(centralServerId, bibId, itemId).isOk();
  }

  @Override
  public void moveItem(UUID centralServerId, String newBibId, Item item) {
    deContributeItem(centralServerId, item);
    contributeItems(centralServerId, newBibId, List.of(item));
  }

  @Override
  public void deContributeItem(UUID centralServerId, Item item) {
    var itemId = item.getHrid();
    log.info("De-contributing item {}", itemId);
    irContributionService.deContributeBibItem(centralServerId, itemId);
  }

  @Override
  public int contributeItems(UUID centralServerId, String bibId, List<Item> items) {
    var bibItems = recordTransformationService.getBibItems(centralServerId, items, (item, e) -> logItemTransformationError(item, e));

    int itemsCount = bibItems.size();

    Assert.isTrue(itemsCount != 0, "Failed to convert items for contribution");

    log.info("Loaded {} items", itemsCount);

    retryTemplate.execute(r -> contributeBibItems(bibId, centralServerId, bibItems));

    log.info("Finished contributing items of bib {}", bibId);

    return itemsCount;
  }

  private void logItemTransformationError(Item item, Exception e) {
    exceptionListener.logWriteError(
      new RuntimeException("Failed to transform inventory item to bib item: " + e.getMessage(), e), item.getId());
  }

  private InnReachResponse contributeBib(UUID centralServerId, String bibId, BibInfo bib) {
    var response = irContributionService.contributeBib(centralServerId, bibId, bib);
    Assert.isTrue(response.isOk(), "Unexpected contribution response: " + response);
    return response;
  }

  private InnReachResponse verifyBibContribution(UUID centralServerId, String bibId) {
    var response = irContributionService.lookUpBib(centralServerId, bibId);
    Assert.isTrue(response.isOk(), "Unexpected verification response: " + response);
    return response;
  }

  private InnReachResponse contributeBibItems(String bibId, UUID centralServerId, List<BibItem> bibItems) {
    var response = irContributionService.contributeBibItems(centralServerId, bibId, BibItemsInfo.of(bibItems));
    Assert.isTrue(response.isOk(), "Unexpected items contribution response: " + response);
    return response;
  }


}
