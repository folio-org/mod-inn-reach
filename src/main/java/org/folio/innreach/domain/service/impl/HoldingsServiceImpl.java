package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.util.ListUtils.getFirstItem;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.HoldingSourcesClient;
import org.folio.innreach.client.HoldingsStorageClient;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.HoldingSourceDTO;

@Service
@RequiredArgsConstructor
public class HoldingsServiceImpl implements HoldingsService {

  private static final String HOLDING_SOURCE_CACHE = "holding-source";

  private final HoldingsStorageClient holdingsStorageClient;
  private final HoldingSourcesClient holdingSourcesClient;

  @Override
  public Holding create(Holding holding) {
    return holdingsStorageClient.createHolding(holding);
  }

  @Override
  public Optional<Holding> find(UUID holdingId) {
    return holdingsStorageClient.findHolding(holdingId);
  }

  @Override
  public Holding update(Holding holding) {
    holdingsStorageClient.updateHolding(holding.getId(), holding);
    return holding;
  }

  @Override
  @Cacheable(cacheNames = HOLDING_SOURCE_CACHE, key = "@folioExecutionContext.tenantId + ': ' + #sourceName")
  public Optional<HoldingSourceDTO> findHoldingSourceByName(String sourceName) {
    return getFirstItem(holdingSourcesClient.querySourceByName(sourceName));
  }

}
