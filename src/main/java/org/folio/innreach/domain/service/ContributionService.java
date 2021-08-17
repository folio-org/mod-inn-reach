package org.folio.innreach.domain.service;

import java.util.UUID;

public interface ContributionService {
  void startInitialContribution(UUID centralServerId);
}
