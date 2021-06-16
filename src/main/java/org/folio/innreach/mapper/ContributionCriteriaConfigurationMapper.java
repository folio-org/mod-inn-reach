package org.folio.innreach.mapper;


import org.folio.innreach.domain.dto.ContributionCriteriaConfigurationDTO;
import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.folio.innreach.dto.Metadata;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.Date;

@Mapper(componentModel = "spring"/*,injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = DateMapper.class*/)
public interface ContributionCriteriaConfigurationMapper {

  ContributionCriteriaConfiguration toEntity(ContributionCriteriaConfigurationDTO criteriaConfigurationDTO);

  @Mapping(target = "metadata.createdDate", source = "criteriaConfiguration.createdDate")
  @Mapping(target = "metadata.createdByUserId", source = "criteriaConfiguration.createdBy")
  @Mapping(target = "metadata.updatedDate", source = "criteriaConfiguration.lastModifiedDate")
  @Mapping(target = "metadata.updatedByUserId", source = "criteriaConfiguration.lastModifiedBy")
  ContributionCriteriaConfigurationDTO toDto(ContributionCriteriaConfiguration criteriaConfiguration);

  default Date OffsetDateTimetoDate(OffsetDateTime entityDate) {
    DateMapper dateMapper=new DateMapper();
    return dateMapper.offsetDateTimeAsDate(entityDate);

  }
}
