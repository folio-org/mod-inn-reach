package org.folio.innreach.domain.service;

import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

public interface ContributionActionService {

  void handleInstanceCreation(Instance newInstance, OngoingContributionStatus ongoingContributionStatus);

  void handleInstanceUpdate(Instance updatedInstance, OngoingContributionStatus ongoingContributionStatus);

  void handleInstanceDelete(Instance deletedInstance, OngoingContributionStatus ongoingContributionStatus);

  void handleItemCreation(Item newItem, OngoingContributionStatus ongoingContributionStatus);

  void handleItemUpdate(Item newItem, Item oldItem, OngoingContributionStatus ongoingContributionStatus);

  void handleItemDelete(Item deletedItem, OngoingContributionStatus ongoingContributionStatus);

  void handleHoldingUpdate(Holding holding, OngoingContributionStatus ongoingContributionStatus);

  void handleHoldingDelete(Holding holding, OngoingContributionStatus ongoingContributionStatus);
}
