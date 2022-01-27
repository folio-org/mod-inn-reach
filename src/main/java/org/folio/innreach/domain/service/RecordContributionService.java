package org.folio.innreach.domain.service;

import java.util.Set;
import java.util.UUID;

import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

public interface RecordContributionService {
  Set<UUID> evaluateInventoryItemForContribution(Item item, Set<UUID> centralServerCodes);

  void decontributeInventoryItemEvents(Item item, Set<UUID> centralServersCodes);

  Set<UUID> evaluateInventoryInstanceForContribution(Instance instance, Set<UUID> centralServerCodes);

  void decontributeInventoryInstanceEvents(Instance instance, Set<UUID> centralServersCodes);

  Set<UUID> evaluateInventoryHoldingForContribution(Holding holding, Set<UUID> centralServerCodes);

  void decontributeInventoryHoldingEvents(Holding holding, Set<UUID> centralServersCodes);
}
