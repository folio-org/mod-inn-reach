package org.folio.innreach.domain.service;

import java.util.List;
import java.util.UUID;

import org.folio.innreach.domain.dto.folio.inventoryStorage.InstanceIterationEvent;
import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.dto.ContributionsDTO;

public interface ContributionService {

  ContributionDTO getCurrent(UUID centralServerId);

  ContributionsDTO getHistory(UUID centralServerId, int offset, int limit);

  void contributeInstances(List<InstanceIterationEvent> events);

  void startInitialContribution(UUID centralServerId);

}
