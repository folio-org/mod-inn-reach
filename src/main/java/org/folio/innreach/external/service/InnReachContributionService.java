package org.folio.innreach.external.service;

import java.util.List;
import java.util.UUID;

import org.folio.innreach.external.dto.Bib;
import org.folio.innreach.external.dto.BibItem;
import org.folio.innreach.external.dto.InnReachResponse;

public interface InnReachContributionService {

  InnReachResponse contributeBib(UUID centralServerId, String bibId, Bib bib);

  InnReachResponse contributeBibItems(UUID centralServerId, String bibId, List<BibItem> bibItems);

  InnReachResponse lookUpBib(UUID centralServerId, String bibId);

}
