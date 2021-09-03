package org.folio.innreach.external.service;

import java.util.UUID;

import org.folio.innreach.external.dto.BibContributionRequest;
import org.folio.innreach.external.dto.InnReachResponse;

public interface InnReachContributionService {

  InnReachResponse contributeBib(UUID centralServerId, String bibId, BibContributionRequest bib);

  InnReachResponse lookUpBib(UUID centralServerId, String bibId);

}
