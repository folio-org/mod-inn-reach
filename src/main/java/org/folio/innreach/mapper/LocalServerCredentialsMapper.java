package org.folio.innreach.mapper;

import org.springframework.stereotype.Component;

import org.folio.innreach.domain.dto.CentralServerDTO;
import org.folio.innreach.domain.entity.LocalServerCredentials;

@Component
public class LocalServerCredentialsMapper {

  public LocalServerCredentials mapToCentralServerCredentials(CentralServerDTO centralServerDTO) {
    if (localServerCredentialsDataIsNull(centralServerDTO)) {
      return null;
    }

    var localServerCredentials = new LocalServerCredentials();
    localServerCredentials.setLocalServerKey(centralServerDTO.getLocalServerKey());
    localServerCredentials.setLocalServerSecret(centralServerDTO.getLocalServerSecret());
    return localServerCredentials;
  }

  private boolean localServerCredentialsDataIsNull(CentralServerDTO centralServerDTO) {
    return centralServerDTO.getLocalServerKey() == null && centralServerDTO.getLocalServerSecret() == null;
  }
}
