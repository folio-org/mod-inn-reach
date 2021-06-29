package org.folio.innreach.mapper;

import org.folio.innreach.domain.dto.ContributionCriteriaExcludedLocationDTO;
import org.folio.innreach.domain.entity.ContributionCriteriaExcludedLocation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContributionCriteriaExcludedLocationMapper {
  ContributionCriteriaExcludedLocation toEntity(ContributionCriteriaExcludedLocationDTO excludedLocationDTO);
  ContributionCriteriaExcludedLocationDTO toDTO(ContributionCriteriaExcludedLocation excludedLocation);
}
