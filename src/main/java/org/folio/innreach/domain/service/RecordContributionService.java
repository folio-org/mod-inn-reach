package org.folio.innreach.domain.service;

import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

public interface RecordContributionService {
  boolean evaluateInventoryItemForContribution(Item item);

  void decontributeInventoryItemEvents(Item item);

  boolean evaluateInventoryInstanceForContribution(Instance instance);

  void decontributeInventoryInstanceEvents(Instance instance);

  boolean evaluateInventoryHoldingForContribution(Holding holding);

  void decontributeInventoryHoldingEvents(Holding holding);
}
