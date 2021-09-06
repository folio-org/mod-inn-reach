package org.folio.innreach.mapper;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.List;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import org.folio.innreach.domain.entity.CentralPatronTypeMapping;
import org.folio.innreach.dto.CentralPatronTypeMappingDTO;
import org.folio.innreach.dto.CentralPatronTypeMappingsDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface CentralPatronTypeMappingMapper {

  CentralPatronTypeMapping toEntity(CentralPatronTypeMappingDTO dto);

  List<CentralPatronTypeMapping> toEntities(Iterable<CentralPatronTypeMappingDTO> dtos);

  @AuditableMapping
  CentralPatronTypeMappingDTO toDTO(CentralPatronTypeMapping entity);

  List<CentralPatronTypeMappingDTO> toDTOs(Iterable<CentralPatronTypeMapping> entities);

  default CentralPatronTypeMappingsDTO toDTOCollection(Page<CentralPatronTypeMapping> pageable) {
    List<CentralPatronTypeMappingDTO> dtos = emptyIfNull(toDTOs(pageable));

    return new CentralPatronTypeMappingsDTO()
      .centralPatronTypeMappings(dtos)
      .totalRecords((int) pageable.getTotalElements());
  }

  default CentralPatronTypeMappingsDTO toDTOCollection(Iterable<CentralPatronTypeMapping> entities) {
    List<CentralPatronTypeMappingDTO> dtos = defaultIfNull(toDTOs(entities), emptyList());

    return new CentralPatronTypeMappingsDTO().centralPatronTypeMappings(dtos).totalRecords(dtos.size());
  }
}
