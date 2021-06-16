package org.folio.innreach.mapper;

import org.folio.innreach.domain.dto.ContributionCriteriaConfigurationDTO;
import org.folio.innreach.domain.dto.ContributionCriteriaExcludedLocationDTO;
import org.folio.innreach.domain.dto.ContributionCriteriaStatisticalCodeBehaviorDTO;
import org.folio.innreach.domain.entity.ContributionBehavior;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.mapstruct.Mapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public class ContributionCriteriaMapper {
  public ContributionCriteriaDTO toContributionCriteriaDTO(ContributionCriteriaConfigurationDTO contributionCriteriaConfigurationDTO) {
    ContributionCriteriaDTO result = new ContributionCriteriaDTO();
    result.setCentralServerId(contributionCriteriaConfigurationDTO.getCentralServerId());
    result.setMetadata(contributionCriteriaConfigurationDTO.getMetadata());
    result.setLocationIds(toLocationIds(contributionCriteriaConfigurationDTO.getExcludedLocations()));
    contributionCriteriaConfigurationDTO.getStatisticalCodeBehaviors().forEach(statisticalCodeBehaviorDTO -> {
      switch (statisticalCodeBehaviorDTO.getContributionBehavior()) {
        case doNotContribute:
          result.setDoNotContributeId(statisticalCodeBehaviorDTO.getStatisticalCodeId());
          break;
        case contributeAsSystemOwned:
          result.setContributeAsSystemOwnedId(statisticalCodeBehaviorDTO.getStatisticalCodeId());
          break;
        case contributeButSuppress:
          result.setContributeButSuppressId(statisticalCodeBehaviorDTO.getStatisticalCodeId());
          break;
        default:
          throw new IllegalArgumentException("The contribution behavior:"+statisticalCodeBehaviorDTO.getContributionBehavior().toString() + "was not defined.");
      }
    });
    return result;
  }

  public ContributionCriteriaConfigurationDTO toContributionCriteriaConfigurationDTO(ContributionCriteriaDTO contributionCriteriaDTO) {
    ContributionCriteriaConfigurationDTO result = new ContributionCriteriaConfigurationDTO();
    result.setExcludedLocations(new HashSet<>());
    result.setStatisticalCodeBehaviors(new HashSet<>());
    result.setCentralServerId(contributionCriteriaDTO.getCentralServerId());
    contributionCriteriaDTO.getLocationIds().forEach(uuid -> {
      ContributionCriteriaExcludedLocationDTO excludedLocationDTO = new ContributionCriteriaExcludedLocationDTO();
      excludedLocationDTO.setExcludedLocationId(uuid);
      result.getExcludedLocations().add(excludedLocationDTO);
    });
    ContributionCriteriaStatisticalCodeBehaviorDTO statisticalCodeBehaviorDTO = new ContributionCriteriaStatisticalCodeBehaviorDTO();
    statisticalCodeBehaviorDTO.setStatisticalCodeId(contributionCriteriaDTO.getDoNotContributeId());
    statisticalCodeBehaviorDTO.setContributionBehavior(ContributionBehavior.doNotContribute);
    result.getStatisticalCodeBehaviors().add(statisticalCodeBehaviorDTO);

    statisticalCodeBehaviorDTO = new ContributionCriteriaStatisticalCodeBehaviorDTO();
    statisticalCodeBehaviorDTO.setStatisticalCodeId(contributionCriteriaDTO.getContributeButSuppressId());
    statisticalCodeBehaviorDTO.setContributionBehavior(ContributionBehavior.contributeButSuppress);
    result.getStatisticalCodeBehaviors().add(statisticalCodeBehaviorDTO);

    statisticalCodeBehaviorDTO = new ContributionCriteriaStatisticalCodeBehaviorDTO();
    statisticalCodeBehaviorDTO.setStatisticalCodeId(contributionCriteriaDTO.getContributeAsSystemOwnedId());
    statisticalCodeBehaviorDTO.setContributionBehavior(ContributionBehavior.contributeAsSystemOwned);
    result.getStatisticalCodeBehaviors().add(statisticalCodeBehaviorDTO);

    return result;
  }


  private List<UUID> toLocationIds(Set<ContributionCriteriaExcludedLocationDTO> excludedLocationDTOS) {
    if (excludedLocationDTOS == null) return null;
    else
      return excludedLocationDTOS.stream()
        .map(excludedLocationDTO -> excludedLocationDTO.getExcludedLocationId())
        .collect(Collectors.toList());
  }

}
