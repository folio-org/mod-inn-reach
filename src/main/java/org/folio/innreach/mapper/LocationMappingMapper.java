package org.folio.innreach.mapper;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.List;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import org.folio.innreach.domain.entity.LocationMapping;
import org.folio.innreach.dto.LocationMappingDTO;
import org.folio.innreach.dto.LocationMappingsDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = DateMapper.class)
public interface LocationMappingMapper {

  @Mapping(target = "innReachLocation.id", source = "dto.innReachLocationId")
  LocationMapping toEntity(LocationMappingDTO dto);

  List<LocationMapping> toEntities(Iterable<LocationMappingDTO> dtos);

  @Mapping(target = "innReachLocationId", source = "entity.innReachLocation.id")
  @Mapping(target = "metadata.createdDate", source = "entity.createdDate")
  @Mapping(target = "metadata.createdByUsername", source = "entity.createdBy")
  @Mapping(target = "metadata.updatedDate", source = "entity.lastModifiedDate")
  @Mapping(target = "metadata.updatedByUsername", source = "entity.lastModifiedBy")
  LocationMappingDTO toDTO(LocationMapping entity);

  List<LocationMappingDTO> toDTOs(Iterable<LocationMapping> entities);

  default LocationMappingsDTO toDTOCollection(Page<LocationMapping> pageable) {
    List<LocationMappingDTO> dtos = defaultIfNull(toDTOs(pageable), emptyList());

    return new LocationMappingsDTO().locationMappings(dtos).totalRecords((int) pageable.getTotalElements());
  }

  default LocationMappingsDTO toDTOCollection(Iterable<LocationMapping> entities) {
    List<LocationMappingDTO> dtos = defaultIfNull(toDTOs(entities), emptyList());

    return new LocationMappingsDTO().locationMappings(dtos).totalRecords(dtos.size());
  }

}
