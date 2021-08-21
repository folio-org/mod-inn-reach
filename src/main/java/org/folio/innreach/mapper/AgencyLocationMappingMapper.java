package org.folio.innreach.mapper;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.Collection;
import java.util.UUID;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.folio.innreach.domain.entity.AgencyLocationAcMapping;
import org.folio.innreach.domain.entity.AgencyLocationLscMapping;
import org.folio.innreach.domain.entity.AgencyLocationMapping;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.dto.AgencyLocationAcMappingDTO;
import org.folio.innreach.dto.AgencyLocationLscMappingDTO;
import org.folio.innreach.dto.AgencyLocationMappingDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface AgencyLocationMappingMapper {

  @Mapping(target = "localServerMappings", source = "dto.localServers")
  @Mapping(target = "id", source = "dto.id", ignore = true)
  AgencyLocationMapping toEntity(AgencyLocationMappingDTO dto);

  @Mapping(target = "localServers", source = "entity.localServerMappings")
  @AuditableMapping
  AgencyLocationMappingDTO toDTO(AgencyLocationMapping entity);

  @Mapping(target = "localServerCode", source = "dto.localCode")
  @Mapping(target = "id", source = "dto.id", ignore = true)
  AgencyLocationLscMapping toEntity(AgencyLocationLscMappingDTO dto);

  Collection<AgencyLocationLscMapping> toEntities(Iterable<AgencyLocationLscMappingDTO> dtos);

  @Mapping(target = "localCode", source = "entity.localServerCode")
  @AuditableMapping
  AgencyLocationLscMappingDTO toDTO(AgencyLocationLscMapping entity);

  Collection<AgencyLocationLscMappingDTO> toDTOs(Iterable<AgencyLocationLscMapping> entities);

  @Mapping(target = "id", source = "dto.id", ignore = true)
  AgencyLocationAcMapping toEntity(AgencyLocationAcMappingDTO dto);

  @AuditableMapping
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
