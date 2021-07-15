package org.folio.innreach.mapper;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.List;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import org.folio.innreach.domain.entity.MaterialTypeMapping;
import org.folio.innreach.dto.MaterialTypeMappingDTO;
import org.folio.innreach.dto.MaterialTypeMappingsDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = DateMapper.class)
public interface MaterialTypeMappingMapper {

	MaterialTypeMapping toEntity(MaterialTypeMappingDTO dto);

  List<MaterialTypeMapping> toEntities(Iterable<MaterialTypeMappingDTO> dtos);
	
  @Mapping(target = "metadata.createdDate", source = "entity.createdDate")
  @Mapping(target = "metadata.createdByUsername", source = "entity.createdBy")
  @Mapping(target = "metadata.updatedDate", source = "entity.lastModifiedDate")
  @Mapping(target = "metadata.updatedByUsername", source = "entity.lastModifiedBy")
  MaterialTypeMappingDTO toDTO(MaterialTypeMapping entity);

  List<MaterialTypeMappingDTO> toDTOs(Iterable<MaterialTypeMapping> entities);

  default MaterialTypeMappingsDTO toDTOCollection(Page<MaterialTypeMapping> pageable) {
    List<MaterialTypeMappingDTO> dtos = defaultIfNull(toDTOs(pageable), emptyList());

    return new MaterialTypeMappingsDTO().materialTypeMappings(dtos).totalRecords((int) pageable.getTotalElements());
  }

  default MaterialTypeMappingsDTO toDTOCollection(Iterable<MaterialTypeMapping> entities) {
    List<MaterialTypeMappingDTO> dtos = defaultIfNull(toDTOs(entities), emptyList());

    return new MaterialTypeMappingsDTO().materialTypeMappings(dtos).totalRecords(dtos.size());
  }

}
