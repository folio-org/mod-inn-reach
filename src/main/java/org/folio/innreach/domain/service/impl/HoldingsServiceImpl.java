package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.util.ListUtils.getFirstItem;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.HoldingSourcesClient;
import org.folio.innreach.client.HoldingsStorageClient;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.HoldingSourceDTO;

@Slf4j
@Service
@RequiredArgsConstructor
public class HoldingsServiceImpl implements HoldingsService {

  private static final String HOLDING_SOURCE_CACHE = "holding-source";

  private final HoldingsStorageClient holdingsStorageClient;
  private final HoldingSourcesClient holdingSourcesClient;

  @Override
  public Holding create(Holding holding) {
    log.debug("create:: parameters holding: {}", holding);
    return holdingsStorageClient.createHolding(holding);
  }

  @Override
  public Optional<Holding> find(UUID holdingId) {
    log.debug("find:: parameters holdingId: {}", holdingId);
    return holdingsStorageClient.findHolding(holdingId);
  }

  @Override
  public void delete(UUID holdingId) {
    log.debug("delete:: parameters holdingId: {}", holdingId);
      holdingsStorageClient.findHolding(holdingId)
        .ifPresentOrElse(holding -> holdingsStorageClient.deleteHolding(holdingId),
          () -> log.info("Holding not found with holdingId:{}", holdingId));
  }

  @Override
  public Holding update(Holding holding) {
    log.debug("update:: parameters holding: {}", holding);
    holdingsStorageClient.updateHolding(holding.getId(), holding);
    return holding;
  }

  @Override
  @Cacheable(cacheNames = HOLDING_SOURCE_CACHE, key = "@folioExecutionContext.tenantId + ': ' + #sourceName")
  public Optional<HoldingSourceDTO> findHoldingSourceByName(String sourceName) {
    log.debug("findHoldingSourceByName:: parameters sourceName: {}", sourceName);
    return getFirstItem(holdingSourcesClient.querySourceByName(sourceName));
  }

}
