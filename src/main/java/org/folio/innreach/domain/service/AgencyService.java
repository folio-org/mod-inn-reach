package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.CentralServerAgenciesDTO;
import org.folio.innreach.dto.LocalServerAgenciesDTO;

public interface AgencyService {

  CentralServerAgenciesDTO getAllAgencies();

  LocalServerAgenciesDTO getLocalServerAgencies(UUID centralServerId);

}
