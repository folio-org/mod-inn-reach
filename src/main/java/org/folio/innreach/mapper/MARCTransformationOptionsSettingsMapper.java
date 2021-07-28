package org.folio.innreach.mapper;

import org.folio.innreach.domain.entity.MARCTransformationOptionsSettings;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsDTO;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = DateMapper.class)
public interface MARCTransformationOptionsSettingsMapper {

  @Mapping(target = "metadata.createdDate", source = "marcTransformOptSet.createdDate")
  @Mapping(target = "metadata.createdByUsername", source = "marcTransformOptSet.createdBy")
  @Mapping(target = "metadata.updatedDate", source = "marcTransformOptSet.lastModifiedDate")
  @Mapping(target = "metadata.updatedByUsername", source = "marcTransformOptSet.lastModifiedBy")
  MARCTransformationOptionsSettingsDTO toMARCTransformationOptSetDto(MARCTransformationOptionsSettings marcTransformOptSet);

  MARCTransformationOptionsSettings toMARCTransformationOptSet(MARCTransformationOptionsSettingsDTO marcTransformOptSetDTO);
}
