package org.folio.innreach.mapper;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.List;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import org.folio.innreach.domain.entity.PatronTypeMapping;
import org.folio.innreach.dto.PatronTypeMappingDTO;
import org.folio.innreach.dto.PatronTypeMappingsDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface PatronTypeMappingMapper {

  PatronTypeMapping toEntity(PatronTypeMappingDTO dto);

  List<PatronTypeMapping> toEntities(Iterable<PatronTypeMappingDTO> dtos);

  @AuditableMapping
  PatronTypeMappingDTO toDTO(PatronTypeMapping entity);

  List<PatronTypeMappingDTO> toDTOs(Iterable<PatronTypeMapping> entities);

  default PatronTypeMappingsDTO toDTOCollection(Page<PatronTypeMapping> pageable) {
    List<PatronTypeMappingDTO> dtos = emptyIfNull(toDTOs(pageable));

    return new PatronTypeMappingsDTO().patronTypeMappings(dtos).totalRecords((int) pageable.getTotalElements());
  }

  default PatronTypeMappingsDTO toDTOCollection(Iterable<PatronTypeMapping> entities) {
    List<PatronTypeMappingDTO> dtos = defaultIfNull(toDTOs(entities), emptyList());

    return new PatronTypeMappingsDTO().patronTypeMappings(dtos).totalRecords(dtos.size());
  }
}
