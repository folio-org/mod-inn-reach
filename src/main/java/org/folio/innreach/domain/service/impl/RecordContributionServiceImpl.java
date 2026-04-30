package org.folio.innreach.domain.service.impl;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.InnReachRetryException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
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
  public void contributeInstance(UUID centralServerId, Instance instance) throws SocketTimeoutException {
    var bibId = instance.getHrid();

    log.info("contributeInstance: contributing bib {}", bibId);

    var bib = recordTransformationService.getBibInfo(centralServerId, instance);

    log.info("contributeInstance: got bib info for bib: {}", bibId);

    contributeAndVerifyBib(centralServerId, bibId, bib);

    log.info("contributeInstance: finished bib {}", bibId);
  }

  @Override
  public void contributeInstanceWithoutRetry(UUID centralServerId, Instance instance) {
    var bibId = instance.getHrid();
    log.info("contributeInstanceWithoutRetry: contributing bib {}", bibId);
    var bib = recordTransformationService.getBibInfo(centralServerId, instance);
    contributeBib(centralServerId, bibId, bib);
  }

  @Override
  public void deContributeInstance(UUID centralServerId, Instance instance) throws SocketTimeoutException {
    var bibId = instance.getHrid();
    log.info("De-contributing bib {}", bibId);
    irContributionService.deContributeBib(centralServerId, bibId);
  }

  private void contributeAndVerifyBib(UUID centralServerId, String bibId, BibInfo bib) {
    log.info("contributeAndVerifyBib: bib id {}", bibId);
    try {
      retryTemplate.execute(() -> contributeBib(centralServerId, bibId, bib));
      retryTemplate.execute(() -> verifyBibContribution(centralServerId, bibId));
      log.info("contributeAndVerifyBib: finished contribution of bib {}", bibId);
    } catch (RetryException ex) {
      log.error("contributeAndVerifyBib:: Failed to contribute bib {} after retries", bibId, ex);
      throw new InnReachRetryException("Contributing bib %s has failed".formatted(bibId), ex.getLastException());
    }
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

    try {
      retryTemplate.execute(() -> contributeBibItems(bibId, centralServerId, bibItems));
    } catch (RetryException ex) {
      log.error("Failed to contribute items for bib {} after retries", bibId, ex);
      throw new InnReachRetryException("Contributing items for bib %s has failed".formatted(bibId), ex.getLastException());
    }

    log.info("Finished contributing items of bib {}", bibId);

    return itemsCount;
  }

  @Override
  public void contributeItemsWithoutRetry(UUID centralServerId, String bibId, List<Item> items) {
    var bibItems = recordTransformationService.getBibItems(centralServerId, items, this::logItemTransformationError);
    int itemsCount = bibItems.size();
    Assert.isTrue(itemsCount != 0, "Failed to convert items for contribution");
    log.info("Loaded {} items", itemsCount);
    contributeBibItems(bibId, centralServerId, bibItems);
  }

  private void logItemTransformationError(Item item, Exception e) {
    exceptionListener.logWriteError(
      new RuntimeException("Failed to transform inventory item to bib item: " + e.getMessage(), e), item.getId());
  }

  private InnReachResponse contributeBib(UUID centralServerId, String bibId, BibInfo bib) {
    log.info("Retry happening for contributeBib with bibId: {}",bibId);
    var response = irContributionService.contributeBib(centralServerId, bibId, bib);
    verifyInnReachContributionResponse(response);
    Assert.isTrue(response.isOk(), "Unexpected contribution response: " + response);
    return response;
  }

  private InnReachResponse verifyBibContribution(UUID centralServerId, String bibId) {
    log.info("verifyBibContribution with bibId: {}",bibId);
    var response = irContributionService.lookUpBib(centralServerId, bibId);
    verifyInnReachContributionResponse(response);
    Assert.isTrue(response.isOk(), "Unexpected verification response: " + response);
    return response;
  }

  private InnReachResponse contributeBibItems(String bibId, UUID centralServerId, List<BibItem> bibItems) {
    var response = irContributionService.contributeBibItems(centralServerId, bibId, BibItemsInfo.of(bibItems));
    verifyInnReachContributionResponse(response);
    Assert.isTrue(response.isOk(), "Unexpected items contribution response: " + response);
    return response;
  }

  private void verifyInnReachContributionResponse(InnReachResponse response) {
    if (response != null && isNotEmpty(response.getErrors())) {
      InnReachResponse.Error errorResponse = response.getErrors().getFirst();

      var errorReason = Optional.ofNullable(errorResponse)
        .map(InnReachResponse.Error::getReason)
        .orElse("");
      if (StringUtils.isNotBlank(errorReason)) {
        log.warn("verifyInnReachContributionResponse:: error reason: {}", errorReason);
      }
      if (errorReason.contains(CONTRIBUTION_IS_CURRENTLY_SUSPENDED)) {
        log.info("Contribution to d2irm is currently suspended error message occurred");
        throw new ServiceSuspendedException(CONTRIBUTION_IS_CURRENTLY_SUSPENDED);
      }

      var errorMessages = Optional.ofNullable(errorResponse)
        .map(InnReachResponse.Error::getMessages)
        .filter(CollectionUtils::isNotEmpty)
        .map(List::getFirst)
        .orElse("");

      if (errorMessages.contains(CONNECTIONS_ALLOWED_FROM_THIS_SERVER)) {
        log.info("Allowable maximum Connection limit error message occurred");
        throw new InnReachConnectionException("Only 5 connections allowed from this server");
      }
    }
  }
}
