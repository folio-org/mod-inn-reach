package org.folio.innreach.mapper;

import org.folio.innreach.domain.entity.PatronTypeMapping;
import org.folio.innreach.dto.PatronTypeMappingDTO;
import org.folio.innreach.dto.PatronTypeMappingsDTO;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = DateMapper.class)
public interface PatronTypeMappingMapper {

  PatronTypeMapping toEntity(PatronTypeMappingDTO dto);

  List<PatronTypeMapping> toEntities(Iterable<PatronTypeMappingDTO> dtos);

  @Mapping(target = "metadata.createdDate", source = "entity.createdDate")
  @Mapping(target = "metadata.createdByUsername", source = "entity.createdBy")
  @Mapping(target = "metadata.updatedDate", source = "entity.lastModifiedDate")
  @Mapping(target = "metadata.updatedByUsername", source = "entity.lastModifiedBy")
  PatronTypeMappingDTO toDTO(PatronTypeMapping entity);

  List<PatronTypeMappingDTO> toDTOs(Iterable<PatronTypeMapping> entities);

  default PatronTypeMappingsDTO toDTOCollection(Page<PatronTypeMapping> pageable) {
    List<PatronTypeMappingDTO> dtos = defaultIfNull(toDTOs(pageable), emptyList());

    return new PatronTypeMappingsDTO().patronTypeMappings(dtos).totalRecords((int) pageable.getTotalElements());
  }

  default PatronTypeMappingsDTO toDTOCollection(Iterable<PatronTypeMapping> entities) {
    List<PatronTypeMappingDTO> dtos = defaultIfNull(toDTOs(entities), emptyList());

    return new PatronTypeMappingsDTO().patronTypeMappings(dtos).totalRecords(dtos.size());
  }
}
