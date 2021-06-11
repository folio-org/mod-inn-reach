package org.folio.innreach.mapper;


import org.folio.innreach.domain.dto.ContributionCriteriaConfigurationDTO;
import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContributionCriteriaConfigurationMapper {

  ContributionCriteriaConfiguration toEntity(ContributionCriteriaConfigurationDTO criteriaConfigurationDTO);

  ContributionCriteriaConfigurationDTO toDto(ContributionCriteriaConfiguration criteriaConfiguration);

}
