package org.folio.innreach.domain.service;

import org.folio.innreach.dto.CentralServerAgenciesDTO;
import org.folio.innreach.dto.CentralServerItemTypesDTO;
import org.folio.innreach.dto.CentralServerPatronTypesDTO;

public interface CentralServerConfigurationService {

  CentralServerAgenciesDTO getAllAgencies();

  CentralServerItemTypesDTO getAllItemTypes();

  CentralServerPatronTypesDTO getAllPatronTypes();

}
