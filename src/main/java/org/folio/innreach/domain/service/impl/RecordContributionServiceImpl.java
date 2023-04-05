package org.folio.innreach.domain.service.impl;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
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

  public static final String CONTRIBUTION_IS_CURRENTLY_SUSPENDED = "is currently suspended";
  public static final String CONNECTIONS_ALLOWED_FROM_THIS_SERVER = "connections allowed from this server";
  @Qualifier("contributionRetryTemplate")
  private final RetryTemplate retryTemplate;
  private final InnReachContributionService irContributionService;
  private final RecordTransformationService recordTransformationService;

  @Qualifier("itemExceptionListener")
  private final ContributionExceptionListener exceptionListener;

  @Override
  public void contributeInstance(UUID centralServerId, Instance instance) throws SocketTimeoutException{
    var bibId = instance.getHrid();

    log.info("contributeInstance: contributing bib {}", bibId);

    var bib = recordTransformationService.getBibInfo(centralServerId, instance);

    log.info("contributeInstance: got bib info for bib: {}", bibId);

    contributeAndVerifyBib(centralServerId, bibId, bib);

    log.info("contributeInstance: finished bib {}", bibId);
  }

  @Override
  public void deContributeInstance(UUID centralServerId, Instance instance) {
    var bibId = instance.getHrid();
    log.info("De-contributing bib {}", bibId);
    irContributionService.deContributeBib(centralServerId, bibId);
  }

  private void contributeAndVerifyBib(UUID centralServerId, String bibId, BibInfo bib) {
    log.info("contributeAndVerifyBib: bib id {}", bibId);
    retryTemplate.execute(r -> contributeBib(centralServerId, bibId, bib));
    retryTemplate.execute(r -> verifyBibContribution(centralServerId, bibId));
    log.info("contributeAndVerifyBib: finished contribution of bib {}", bibId);
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
  public void moveItem(UUID centralServerId, String newBibId, Item item) throws SocketTimeoutException {
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
  public int contributeItems(UUID centralServerId, String bibId, List<Item> items) throws SocketTimeoutException {
    var bibItems = recordTransformationService.getBibItems(centralServerId, items, this::logItemTransformationError);

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
    log.info("Retry happening for contributeBib with bibId: {}",bibId);
    var response = irContributionService.contributeBib(centralServerId, bibId, bib);
    checkServiceSuspension(response);
    Assert.isTrue(response.isOk(), "Unexpected contribution response: " + response);
    return response;
  }

  private InnReachResponse verifyBibContribution(UUID centralServerId, String bibId) {
    log.info("verifyBibContribution with bibId: {}",bibId);
    var response = irContributionService.lookUpBib(centralServerId, bibId);
    checkServiceSuspension(response);
    Assert.isTrue(response.isOk(), "Unexpected verification response: " + response);
    return response;
  }

  private InnReachResponse contributeBibItems(String bibId, UUID centralServerId, List<BibItem> bibItems) {
    var response = irContributionService.contributeBibItems(centralServerId, bibId, BibItemsInfo.of(bibItems));
    checkServiceSuspension(response);
    Assert.isTrue(response.isOk(), "Unexpected items contribution response: " + response);
    return response;
  }

  private void checkServiceSuspension(InnReachResponse response) {
    if (response != null && response.getErrors() != null && !response.getErrors().isEmpty()) {
      InnReachResponse.Error errorResponse = response.getErrors().get(0);

      var error = errorResponse!=null ? errorResponse.getReason() : "";
      String errorMessages = "";

      if (errorResponse!=null && errorResponse.getMessages()!=null && !errorResponse.getMessages().isEmpty()) {
        errorMessages = errorResponse.getMessages().get(0);
      }
      log.info("checkServiceSuspension error: {}", error);
      if (error.contains(CONTRIBUTION_IS_CURRENTLY_SUSPENDED)) {
        log.info("Contribution to d2irm is currently suspended error message occurred");
        throw new ServiceSuspendedException(CONTRIBUTION_IS_CURRENTLY_SUSPENDED);
      }
      if (errorMessages.contains(CONNECTIONS_ALLOWED_FROM_THIS_SERVER)) {
        log.info("Allowable maximum Connection limit error message occurred");
        throw new InnReachConnectionException("Only 5 connections allowed from this server");
      }
    }
  }
}
