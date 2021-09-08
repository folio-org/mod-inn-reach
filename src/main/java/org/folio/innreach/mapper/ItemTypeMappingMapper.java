package org.folio.innreach.mapper;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.List;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import org.folio.innreach.domain.entity.ItemTypeMapping;
import org.folio.innreach.dto.ItemTypeMappingDTO;
import org.folio.innreach.dto.ItemTypeMappingsDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface ItemTypeMappingMapper {
  ItemTypeMapping toEntity(ItemTypeMappingDTO dto);

  List<ItemTypeMapping> toEntities(Iterable<ItemTypeMappingDTO> dtos);

  @AuditableMapping
  ItemTypeMappingDTO toDTO(ItemTypeMapping entity);

  List<ItemTypeMappingDTO> toDTOs(Iterable<ItemTypeMapping> entities);

  default ItemTypeMappingsDTO toDTOCollection(Page<ItemTypeMapping> pageable) {
    List<ItemTypeMappingDTO> dtos = emptyIfNull(toDTOs(pageable));

    return new ItemTypeMappingsDTO().itemTypeMappings(dtos).totalRecords((int) pageable.getTotalElements());
  }

  default ItemTypeMappingsDTO toDTOCollection(Iterable<ItemTypeMapping> entities) {
    List<ItemTypeMappingDTO> dtos = defaultIfNull(toDTOs(entities), emptyList());

    return new ItemTypeMappingsDTO().itemTypeMappings(dtos).totalRecords(dtos.size());
  }
}
