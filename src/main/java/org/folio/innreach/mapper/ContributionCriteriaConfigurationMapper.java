package org.folio.innreach.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.folio.innreach.dto.ContributionCriteriaDTO;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = DateMapper.class)
public interface ContributionCriteriaConfigurationMapper {

  ContributionCriteriaConfiguration toEntity(ContributionCriteriaDTO criteriaConfigurationDTO);

  @Mapping(target = "metadata.createdDate", source = "entity.createdDate")
  @Mapping(target = "metadata.createdByUsername", source = "entity.createdBy")
  @Mapping(target = "metadata.updatedDate", source = "entity.lastModifiedDate")
  @Mapping(target = "metadata.updatedByUsername", source = "entity.lastModifiedBy")
  ContributionCriteriaDTO toDTO(ContributionCriteriaConfiguration entity);

}
