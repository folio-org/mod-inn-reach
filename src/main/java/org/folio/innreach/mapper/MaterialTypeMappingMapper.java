package org.folio.innreach.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.folio.innreach.domain.entity.MaterialTypeMapping;
import org.folio.innreach.dto.MaterialTypeMappingDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = DateMapper.class)
public interface MaterialTypeMappingMapper {

	MaterialTypeMapping mapToEntity(MaterialTypeMappingDTO dto);

  @Mapping(target = "metadata.createdDate", source = "entity.createdDate")
  @Mapping(target = "metadata.createdByUserId", source = "entity.createdBy")
  @Mapping(target = "metadata.updatedDate", source = "entity.lastModifiedDate")
  @Mapping(target = "metadata.updatedByUserId", source = "entity.lastModifiedBy")
  MaterialTypeMappingDTO mapToDTO(MaterialTypeMapping entity);

}
