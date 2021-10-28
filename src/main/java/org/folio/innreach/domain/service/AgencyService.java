package org.folio.innreach.domain.service;

import java.util.List;
import java.util.UUID;

import org.folio.innreach.dto.CentralServerAgenciesDTO;
import org.folio.innreach.dto.LocalServer;

public interface AgencyService {

  CentralServerAgenciesDTO getAllAgencies();

  List<LocalServer> getLocalServers(UUID centralServerId);

}
