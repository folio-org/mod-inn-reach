package org.folio.innreach.mapper;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.List;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import org.folio.innreach.domain.entity.LibraryMapping;
import org.folio.innreach.dto.LibraryMappingDTO;
import org.folio.innreach.dto.LibraryMappingsDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = DateMapper.class)
public interface LibraryMappingMapper {

  LibraryMapping toEntity(LibraryMappingDTO dto);

  List<LibraryMapping> toEntities(Iterable<LibraryMappingDTO> dtos);

  @Mapping(target = "metadata.createdDate", source = "entity.createdDate")
  @Mapping(target = "metadata.createdByUserId", source = "entity.createdBy")
  @Mapping(target = "metadata.updatedDate", source = "entity.lastModifiedDate")
  @Mapping(target = "metadata.updatedByUserId", source = "entity.lastModifiedBy")
  LibraryMappingDTO toDTO(LibraryMapping entity);

  List<LibraryMappingDTO> toDTOs(Iterable<LibraryMapping> entities);

  default LibraryMappingsDTO toDTOCollection(Page<LibraryMapping> pageable) {
    List<LibraryMappingDTO> dtos = defaultIfNull(toDTOs(pageable), emptyList());

    return new LibraryMappingsDTO().libraryMappings(dtos).totalRecords((int) pageable.getTotalElements());
  }

  default LibraryMappingsDTO toDTOCollection(Iterable<LibraryMapping> entities) {
    List<LibraryMappingDTO> dtos = defaultIfNull(toDTOs(entities), emptyList());

    return new LibraryMappingsDTO().libraryMappings(dtos).totalRecords(dtos.size());
  }

}
