package org.folio.innreach.mapper;

import org.folio.innreach.domain.dto.CentralServerDTO;
import org.folio.innreach.domain.dto.LocalAgencyDTO;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.LocalAgency;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface CentralServerMapper {

  @Mappings({
    @Mapping(target = "centralServerCredentials.centralServerKey", source = "centralServerKey"),
    @Mapping(target = "centralServerCredentials.centralServerSecret", source = "centralServerSecret"),
    @Mapping(target = "localServerCredentials.localServerKey", source = "localServerKey"),
    @Mapping(target = "localServerCredentials.localServerSecret", source = "localServerSecret"),
  })
  CentralServer mapToCentralServer(CentralServerDTO centralServerDTO);

  LocalAgency mapToLocalAgency(LocalAgencyDTO localAgencyDTO);

  @Mappings({
    @Mapping(target = "centralServerKey", source = "centralServerCredentials.centralServerKey"),
    @Mapping(target = "centralServerSecret", source = "centralServerCredentials.centralServerSecret"),
    @Mapping(target = "localServerKey", source = "localServerCredentials.localServerKey"),
    @Mapping(target = "localServerSecret", source = "localServerCredentials.localServerSecret")
  })
  CentralServerDTO mapToCentralServerDTO(CentralServer centralServer);

  LocalAgencyDTO mapToLocalAgencyDTO(LocalAgency localAgency);
}
