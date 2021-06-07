package org.folio.innreach.mapper;

import org.folio.innreach.domain.dto.ContributionCriteriaStatisticalCodeBehaviorDTO;
import org.folio.innreach.domain.entity.ContributionCriteriaStatisticalCodeBehavior;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContributionCriteriaStatisticalCodeBehaviorMapper {
  ContributionCriteriaStatisticalCodeBehavior toEntity(ContributionCriteriaStatisticalCodeBehaviorDTO statisticalCodeBehaviorDTO);
  ContributionCriteriaStatisticalCodeBehaviorDTO toDTO(ContributionCriteriaStatisticalCodeBehavior statisticalCodeBehavior);
}
