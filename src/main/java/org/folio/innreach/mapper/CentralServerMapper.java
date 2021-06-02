package org.folio.innreach.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.folio.innreach.domain.dto.CentralServerDTO;
import org.folio.innreach.domain.dto.LocalAgencyDTO;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.LocalAgency;
import org.folio.innreach.domain.entity.LocalServerCredentials;

@Mapper(componentModel = "spring")
public interface CentralServerMapper {

  @Mapping(target = "centralServerCredentials.centralServerKey", source = "centralServerKey")
  @Mapping(target = "centralServerCredentials.centralServerSecret", source = "centralServerSecret")
  @Mapping(target = "localServerCredentials", expression = "java(mapToLocalServerCredentials(centralServerDTO))")
  CentralServer mapToCentralServer(CentralServerDTO centralServerDTO);

  default LocalServerCredentials mapToLocalServerCredentials(CentralServerDTO centralServerDTO) {
    if (centralServerDTO.getLocalServerKey() == null && centralServerDTO.getLocalServerSecret() == null) {
      return null;
    }

    var localServerCredentials = new LocalServerCredentials();
    localServerCredentials.setLocalServerKey(centralServerDTO.getLocalServerKey());
    localServerCredentials.setLocalServerSecret(centralServerDTO.getLocalServerSecret());
    return localServerCredentials;
  }

  LocalAgency mapToLocalAgency(LocalAgencyDTO localAgencyDTO);

  @Mapping(target = "centralServerKey", source = "centralServerCredentials.centralServerKey")
  @Mapping(target = "centralServerSecret", source = "centralServerCredentials.centralServerSecret")
  @Mapping(target = "localServerKey", source = "localServerCredentials.localServerKey")
  @Mapping(target = "localServerSecret", source = "localServerCredentials.localServerSecret")
  CentralServerDTO mapToCentralServerDTO(CentralServer centralServer);

  LocalAgencyDTO mapToLocalAgencyDTO(LocalAgency localAgency);
}
