package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.dto.ContributionsDTO;

public interface ContributionService {

  ContributionDTO getCurrent(UUID centralServerId);

  ContributionsDTO getHistory(UUID centralServerId, int offset, int limit);

  void startInitialContribution(UUID centralServerId);
}
