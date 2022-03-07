package org.folio.innreach.mapper;

import java.util.List;

import org.folio.innreach.domain.entity.LocationMapping;
import org.folio.innreach.dto.LocationMappingForAllLibrariesDTO;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface LocationMappingForAllLibrariesMapper {

  @Mapping(target = "innReachLocation.id", source = "dto.innReachLocationId")
  LocationMapping toEntity(LocationMappingForAllLibrariesDTO dto);

  List<LocationMapping> toEntities(Iterable<LocationMappingForAllLibrariesDTO> dtos);

  @Mapping(target = "innReachLocationId", source = "entity.innReachLocation.id")
  @AuditableMapping
  LocationMappingForAllLibrariesDTO toDTO(LocationMapping entity);

  List<LocationMappingForAllLibrariesDTO> toDTOs(Iterable<LocationMapping> entities);

}
