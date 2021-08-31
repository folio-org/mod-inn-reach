package org.folio.innreach.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

import org.folio.innreach.domain.entity.MARCTransformationOptionsSettings;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = MappingMethods.class)
public interface MARCTransformationOptionsSettingsMapper {

  @AuditableMapping
  MARCTransformationOptionsSettingsDTO toDto(MARCTransformationOptionsSettings marcTransformOptSet);

  MARCTransformationOptionsSettings toEntity(MARCTransformationOptionsSettingsDTO marcTransformOptSetDTO);
}
