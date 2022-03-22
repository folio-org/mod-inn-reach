package org.folio.innreach.domain.service;

import java.util.Optional;
import java.util.UUID;

import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.HoldingSourceDTO;

public interface HoldingsService extends BasicService<UUID, Holding> {
  Optional<HoldingSourceDTO> findHoldingSourceByName(String sourceName);
}
