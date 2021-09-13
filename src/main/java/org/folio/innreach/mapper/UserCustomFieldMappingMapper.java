package org.folio.innreach.mapper;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.List;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import org.folio.innreach.domain.entity.UserCustomFieldMapping;
import org.folio.innreach.dto.UserCustomFieldMappingDTO;
import org.folio.innreach.dto.UserCustomFieldMappingsDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface UserCustomFieldMappingMapper {

  UserCustomFieldMapping toEntity(UserCustomFieldMappingDTO dto);

  List<UserCustomFieldMapping> toEntities(Iterable<UserCustomFieldMappingDTO> dtos);

  @AuditableMapping
  UserCustomFieldMappingDTO toDTO(UserCustomFieldMapping entity);

  List<UserCustomFieldMappingDTO> toDTOs(Iterable<UserCustomFieldMapping> entities);

  default UserCustomFieldMappingsDTO toDTOCollection(Page<UserCustomFieldMapping> pageable) {
    List<UserCustomFieldMappingDTO> dtos = emptyIfNull(toDTOs(pageable));

    return new UserCustomFieldMappingsDTO().userCustomFieldMappings(dtos).totalRecords((int) pageable.getTotalElements());
  }

  default UserCustomFieldMappingsDTO toDTOCollection(Iterable<UserCustomFieldMapping> entities) {
    List<UserCustomFieldMappingDTO> dtos = defaultIfNull(toDTOs(entities), emptyList());

    return new UserCustomFieldMappingsDTO().userCustomFieldMappings(dtos).totalRecords(dtos.size());
  }
}
