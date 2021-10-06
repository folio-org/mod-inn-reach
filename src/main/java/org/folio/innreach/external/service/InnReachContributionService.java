package org.folio.innreach.external.service;

import java.util.UUID;

import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.external.dto.BibItemsInfo;
import org.folio.innreach.external.dto.InnReachResponse;

public interface InnReachContributionService {

  InnReachResponse contributeBib(UUID centralServerId, String bibId, BibInfo bib);

  InnReachResponse contributeBibItems(UUID centralServerId, String bibId, BibItemsInfo bibItems);

  InnReachResponse lookUpBib(UUID centralServerId, String bibId);

}
