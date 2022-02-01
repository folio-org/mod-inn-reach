package org.folio.innreach.domain.service;

import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

import java.util.List;

public interface EvaluateService {

  void handleItemEvent(Item item, List<String> centralServerCodes);
  void handleHoldingEvent(Holding holding, List<String> centralServerCodes);
  void handleInstanceEvent(Instance instance, List<String> centralServerCodes);
}
