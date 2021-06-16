package org.folio.innreach.mapper;

import org.folio.innreach.domain.dto.ContributionCriteriaConfigurationDTO;
import org.folio.innreach.domain.dto.ContributionCriteriaExcludedLocationDTO;
import org.folio.innreach.domain.dto.ContributionCriteriaStatisticalCodeBehaviorDTO;
import org.folio.innreach.domain.entity.ContributionBehavior;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public class ContributionCriteriaMapper {
  public ContributionCriteriaDTO toContributionCriteriaDTO(ContributionCriteriaConfigurationDTO contributionCriteriaConfigurationDTO) {
    var contributionCriteriaDTO = new ContributionCriteriaDTO();
    contributionCriteriaDTO.setCentralServerId(contributionCriteriaConfigurationDTO.getCentralServerId());
    contributionCriteriaDTO.setMetadata(contributionCriteriaConfigurationDTO.getMetadata());
    contributionCriteriaDTO.setLocationIds(toLocationIds(contributionCriteriaConfigurationDTO.getExcludedLocations()));
    contributionCriteriaConfigurationDTO.getStatisticalCodeBehaviors().forEach(statisticalCodeBehaviorDTO -> {
      switch (statisticalCodeBehaviorDTO.getContributionBehavior()) {
        case doNotContribute:
          contributionCriteriaDTO.setDoNotContributeId(statisticalCodeBehaviorDTO.getStatisticalCodeId());
          break;
        case contributeAsSystemOwned:
          contributionCriteriaDTO.setContributeAsSystemOwnedId(statisticalCodeBehaviorDTO.getStatisticalCodeId());
          break;
        case contributeButSuppress:
          contributionCriteriaDTO.setContributeButSuppressId(statisticalCodeBehaviorDTO.getStatisticalCodeId());
          break;
        default:
          throw new IllegalArgumentException("The contribution behavior:"+statisticalCodeBehaviorDTO.getContributionBehavior().toString() + "was not defined.");
      }
    });
    return contributionCriteriaDTO;
  }

  public ContributionCriteriaConfigurationDTO toContributionCriteriaConfigurationDTO(ContributionCriteriaDTO contributionCriteriaDTO) {
    var contributionCriteriaConfigurationDTO = new ContributionCriteriaConfigurationDTO();
    contributionCriteriaConfigurationDTO.setExcludedLocations(new HashSet<>());
    contributionCriteriaConfigurationDTO.setStatisticalCodeBehaviors(new HashSet<>());
    contributionCriteriaConfigurationDTO.setCentralServerId(contributionCriteriaDTO.getCentralServerId());
    contributionCriteriaDTO.getLocationIds().forEach(uuid -> {
      var excludedLocationDTO = new ContributionCriteriaExcludedLocationDTO();
      excludedLocationDTO.setExcludedLocationId(uuid);
      contributionCriteriaConfigurationDTO.getExcludedLocations().add(excludedLocationDTO);
    });
    var statisticalCodeBehaviorDTO = new ContributionCriteriaStatisticalCodeBehaviorDTO();
    statisticalCodeBehaviorDTO.setStatisticalCodeId(contributionCriteriaDTO.getDoNotContributeId());
    statisticalCodeBehaviorDTO.setContributionBehavior(ContributionBehavior.doNotContribute);
    contributionCriteriaConfigurationDTO.getStatisticalCodeBehaviors().add(statisticalCodeBehaviorDTO);

    statisticalCodeBehaviorDTO = new ContributionCriteriaStatisticalCodeBehaviorDTO();
    statisticalCodeBehaviorDTO.setStatisticalCodeId(contributionCriteriaDTO.getContributeButSuppressId());
    statisticalCodeBehaviorDTO.setContributionBehavior(ContributionBehavior.contributeButSuppress);
    contributionCriteriaConfigurationDTO.getStatisticalCodeBehaviors().add(statisticalCodeBehaviorDTO);

    statisticalCodeBehaviorDTO = new ContributionCriteriaStatisticalCodeBehaviorDTO();
    statisticalCodeBehaviorDTO.setStatisticalCodeId(contributionCriteriaDTO.getContributeAsSystemOwnedId());
    statisticalCodeBehaviorDTO.setContributionBehavior(ContributionBehavior.contributeAsSystemOwned);
    contributionCriteriaConfigurationDTO.getStatisticalCodeBehaviors().add(statisticalCodeBehaviorDTO);

    return contributionCriteriaConfigurationDTO;
  }

  private List<UUID> toLocationIds(Set<ContributionCriteriaExcludedLocationDTO> excludedLocationDTOS) {
    if (excludedLocationDTOS == null) return new ArrayList<>();
    else
      return excludedLocationDTOS.stream()
        .map(ContributionCriteriaExcludedLocationDTO::getExcludedLocationId)
        .collect(Collectors.toList());
  }
}
