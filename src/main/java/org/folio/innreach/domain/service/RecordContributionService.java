package org.folio.innreach.domain.service;

import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

public interface RecordContributionService {

  void decontributeInventoryItemEvents(Item item);

  void decontributeInventoryInstanceEvents(Instance instance);

  void decontributeInventoryHoldingEvents(Holding holding);

  void updateInventoryItem(Item oldItem, Item newItem);

  void updateInventoryInstance(Instance oldEntity, Instance newEntity);

  void updateInventoryHolding(Holding oldEntity, Holding newEntity);
}
