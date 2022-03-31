package org.folio.innreach.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration;
import org.folio.innreach.dto.VisiblePatronFieldConfigurationDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface VisiblePatronFieldConfigurationMapper {

  VisiblePatronFieldConfiguration toEntity(VisiblePatronFieldConfigurationDTO dto);

  @AuditableMapping
  VisiblePatronFieldConfigurationDTO toDTO(VisiblePatronFieldConfiguration entity);
}
