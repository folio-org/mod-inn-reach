package org.folio.innreach.domain.service;

import org.folio.innreach.dto.CentralServerAgenciesDTO;
import org.folio.innreach.dto.CentralServerItemTypesDTO;

public interface CentralServerConfigurationService {

  CentralServerAgenciesDTO getAllAgencies();

  CentralServerItemTypesDTO getAllItemTypes();

}
