package org.folio.innreach.mapper;

import org.folio.innreach.domain.entity.AgencyLocationAcMapping;
import org.folio.innreach.domain.entity.AgencyLocationLscMapping;
import org.folio.innreach.domain.entity.AgencyLocationMapping;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.dto.AgencyLocationAcMappingDTO;
import org.folio.innreach.dto.AgencyLocationLscMappingDTO;
import org.folio.innreach.dto.AgencyLocationMappingDTO;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = DateMapper.class)
public interface AgencyLocationMappingMapper {

  @Mapping(target = "localServerMappings", source = "dto.localServers")
  @Mapping(target = "id", source = "dto.id", ignore = true)
  AgencyLocationMapping toEntity(AgencyLocationMappingDTO dto);

  @Mapping(target = "localServers", source = "entity.localServerMappings")
  @Mapping(target = "metadata.createdDate", source = "entity.createdDate")
  @Mapping(target = "metadata.createdByUsername", source = "entity.createdBy")
  @Mapping(target = "metadata.updatedDate", source = "entity.lastModifiedDate")
  @Mapping(target = "metadata.updatedByUsername", source = "entity.lastModifiedBy")
  AgencyLocationMappingDTO toDTO(AgencyLocationMapping entity);

  @Mapping(target = "localServerCode", source = "dto.localCode")
  @Mapping(target = "id", source = "dto.id", ignore = true)
  AgencyLocationLscMapping toEntity(AgencyLocationLscMappingDTO dto);

  Collection<AgencyLocationLscMapping> toEntities(Iterable<AgencyLocationLscMappingDTO> dtos);

  @Mapping(target = "localCode", source = "entity.localServerCode")
  @Mapping(target = "metadata.createdDate", source = "entity.createdDate")
  @Mapping(target = "metadata.createdByUsername", source = "entity.createdBy")
  @Mapping(target = "metadata.updatedDate", source = "entity.lastModifiedDate")
  @Mapping(target = "metadata.updatedByUsername", source = "entity.lastModifiedBy")
  AgencyLocationLscMappingDTO toDTO(AgencyLocationLscMapping entity);

  Collection<AgencyLocationLscMappingDTO> toDTOs(Iterable<AgencyLocationLscMapping> entities);

  @Mapping(target = "id", source = "dto.id", ignore = true)
  AgencyLocationAcMapping toEntity(AgencyLocationAcMappingDTO dto);

  @Mapping(target = "metadata.createdDate", source = "entity.createdDate")
  @Mapping(target = "metadata.createdByUsername", source = "entity.createdBy")
  @Mapping(target = "metadata.updatedDate", source = "entity.lastModifiedDate")
  @Mapping(target = "metadata.updatedByUsername", source = "entity.lastModifiedBy")
  AgencyLocationAcMappingDTO toDTO(AgencyLocationAcMapping entity);

  default AgencyLocationMapping toEntityWithRefs(AgencyLocationMappingDTO dto, UUID centralServerId) {
    var entity = toEntity(dto);

    emptyIfNull(entity.getLocalServerMappings())
      .forEach(lsm -> {
        lsm.setCentralServerMapping(entity);

        emptyIfNull(lsm.getAgencyCodeMappings())
          .forEach(acm -> acm.setLocalServerMapping(lsm));
      });

    var server = new CentralServer();
    server.setId(centralServerId);
    entity.setCentralServer(server);

    return entity;
  }

}
