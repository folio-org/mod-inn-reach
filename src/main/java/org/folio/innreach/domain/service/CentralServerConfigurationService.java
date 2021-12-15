package org.folio.innreach.domain.service;

import java.util.List;
import java.util.UUID;

import org.folio.innreach.dto.CentralServerAgenciesDTO;
import org.folio.innreach.dto.CentralServerItemTypesDTO;
import org.folio.innreach.dto.CentralServerPatronTypesDTO;
import org.folio.innreach.dto.LocalServer;

public interface CentralServerConfigurationService {

  CentralServerAgenciesDTO getAllAgencies();

  CentralServerItemTypesDTO getAllItemTypes();

  CentralServerPatronTypesDTO getAllPatronTypes();

  List<LocalServer> getLocalServers(UUID centralServerId);
}
