package org.folio.innreach.mapper;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.List;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import org.folio.innreach.domain.entity.LocationMapping;
import org.folio.innreach.dto.LocationMappingForOneLibraryDTO;
import org.folio.innreach.dto.LocationMappingsForOneLibraryDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface LocationMappingMapper {

  @Mapping(target = "innReachLocation.id", source = "dto.innReachLocationId")
  LocationMapping toEntity(LocationMappingForOneLibraryDTO dto);

  List<LocationMapping> toEntities(Iterable<LocationMappingForOneLibraryDTO> dtos);

  @Mapping(target = "innReachLocationId", source = "entity.innReachLocation.id")
  @AuditableMapping
  LocationMappingForOneLibraryDTO toDTO(LocationMapping entity);

  List<LocationMappingForOneLibraryDTO> toDTOs(Iterable<LocationMapping> entities);

  default LocationMappingsForOneLibraryDTO toDTOCollection(Page<LocationMapping> pageable) {
    List<LocationMappingForOneLibraryDTO> dtos = defaultIfNull(toDTOs(pageable), emptyList());

    return new LocationMappingsForOneLibraryDTO().locationMappings(dtos).totalRecords((int) pageable.getTotalElements());
  }

  default LocationMappingsForOneLibraryDTO toDTOCollection(Iterable<LocationMapping> entities) {
    List<LocationMappingForOneLibraryDTO> dtos = defaultIfNull(toDTOs(entities), emptyList());

    return new LocationMappingsForOneLibraryDTO().locationMappings(dtos).totalRecords(dtos.size());
  }

}
