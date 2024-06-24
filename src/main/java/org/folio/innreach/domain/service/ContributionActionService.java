package org.folio.innreach.domain.service;

import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.StorageLoanDTO;

public interface ContributionActionService {

  void handleInstanceCreation(Instance newInstance);

  void handleInstanceUpdate(Instance updatedInstance);

  void handleInstanceDelete(Instance deletedInstance);

  void handleItemCreation(Item newItem, OngoingContributionStatus ongoingContributionStatus);

  void handleItemUpdate(Item newItem, Item oldItem, OngoingContributionStatus ongoingContributionStatus);

  void handleItemDelete(Item deletedItem, OngoingContributionStatus ongoingContributionStatus);

  void handleLoanCreation(StorageLoanDTO loan);

  void handleLoanUpdate(StorageLoanDTO loan);

  void handleRequestChange(RequestDTO request);

  void handleHoldingUpdate(Holding holding, OngoingContributionStatus ongoingContributionStatus);

  void handleHoldingDelete(Holding holding, OngoingContributionStatus ongoingContributionStatus);
}
