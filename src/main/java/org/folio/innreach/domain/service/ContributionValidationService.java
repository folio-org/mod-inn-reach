package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.ContributionDTO;

public interface ContributionValidationService {

  void validate(UUID centralServerId, ContributionDTO contribution);

}
