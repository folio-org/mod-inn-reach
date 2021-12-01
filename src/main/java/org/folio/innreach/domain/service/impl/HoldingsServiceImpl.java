package org.folio.innreach.domain.service.impl;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.HoldingsStorageClient;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.dto.Holding;

@Service
@RequiredArgsConstructor
public class HoldingsServiceImpl implements HoldingsService {

  private final HoldingsStorageClient holdingsStorageClient;

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
    return holdingsStorageClient.updateHolding(holding.getId(), holding);
  }

}