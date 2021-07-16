package org.folio.innreach.external.service;

import org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO;
import org.folio.innreach.external.dto.AccessTokenDTO;

public interface InnReachAuthExternalService {

  AccessTokenDTO getAccessToken(CentralServerConnectionDetailsDTO connectionDetailsDTO);
}
