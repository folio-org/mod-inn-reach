package org.folio.innreach.external.service;

import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.external.dto.AccessTokenRequestDTO;

public interface InnReachExternalService {
  AccessTokenDTO getAccessToken(AccessTokenRequestDTO tokenRequestDTO);
}
