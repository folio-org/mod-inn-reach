package org.folio.innreach.mapper;

import org.folio.innreach.domain.dto.CentralServerDTO;
import org.folio.innreach.domain.dto.ContributionCriteriaConfigurationDTO;
import org.folio.innreach.domain.dto.ContributionCriteriaExcludedLocationDTO;
import org.folio.innreach.domain.dto.ContributionCriteriaStatisticalCodeBehaviorDTO;
import org.folio.innreach.domain.dto.LocalAgencyDTO;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.folio.innreach.domain.entity.ContributionCriteriaExcludedLocation;
import org.folio.innreach.domain.entity.ContributionCriteriaStatisticalCodeBehavior;
import org.folio.innreach.domain.entity.LocalAgency;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import javax.persistence.MapsId;

@Mapper(componentModel = "spring")
public interface ContributionCriteriaConfigurationMapper {

  ContributionCriteriaConfiguration toEntity(ContributionCriteriaConfigurationDTO criteriaConfigurationDTO);
  ContributionCriteriaConfigurationDTO toDto(ContributionCriteriaConfiguration criteriaConfiguration);

}
