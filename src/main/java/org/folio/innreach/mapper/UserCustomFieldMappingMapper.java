package org.folio.innreach.mapper;

import org.folio.innreach.domain.entity.UserCustomFieldMapping;
import org.folio.innreach.dto.UserCustomFieldMappingDTO;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface UserCustomFieldMappingMapper {

  UserCustomFieldMapping toEntity(UserCustomFieldMappingDTO dto);

  @AuditableMapping
  UserCustomFieldMappingDTO toDTO(UserCustomFieldMapping entity);
}
