package org.folio.innreach.mapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.LocalAgency;
import org.folio.innreach.domain.entity.LocalServerCredentials;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.LocalAgencyDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
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

  @Mapping(target = "folioLibraries", expression = "java(mapToFolioLibraries(localAgencyDTO))")
  LocalAgency mapToLocalAgency(LocalAgencyDTO localAgencyDTO);

  default List<LocalAgency.FolioLibrary> mapToFolioLibraries(LocalAgencyDTO localAgencyDTO) {
    var libraryIds = localAgencyDTO.getFolioLibraryIds();
    return libraryIds.stream().map(id -> new LocalAgency.FolioLibrary(id, null)).collect(Collectors.toList());
  }

  @Mapping(target = "centralServerKey", source = "centralServerCredentials.centralServerKey")
  @Mapping(target = "centralServerSecret", source = "centralServerCredentials.centralServerSecret")
  @Mapping(target = "localServerKey", source = "localServerCredentials.localServerKey")
  @Mapping(target = "localServerSecret", source = "localServerCredentials.localServerSecret")
  @AuditableMapping
  CentralServerDTO mapToCentralServerDTO(CentralServer centralServer);

  @Mapping(target = "folioLibraryIds", expression = "java(mapToFolioLibraryIds(localAgency))")
  LocalAgencyDTO mapToLocalAgencyDTO(LocalAgency localAgency);

  default List<UUID> mapToFolioLibraryIds(LocalAgency localAgency) {
    return localAgency.getFolioLibraries().stream().map(LocalAgency.FolioLibrary::getFolioLibraryId).collect(Collectors.toList());
  }
}
