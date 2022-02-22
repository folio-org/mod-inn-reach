package org.folio.innreach.domain.service;

import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.StorageLoanDTO;

public interface ContributionActionService {

  void handleInstanceCreation(Instance newInstance);

  void handleInstanceUpdate(Instance updatedInstance);

  void handleInstanceDelete(Instance deletedInstance);

  void handleItemCreation(Item newItem);

  void handleItemUpdate(Item newItem, Item oldItem);

  void handleItemDelete(Item deletedItem);

  void handleLoanUpdate(StorageLoanDTO loan);

  void handleRequestChange(RequestDTO request);

  void handleHoldingUpdate(Holding holding);

  void handleHoldingDelete(Holding holding);
}
