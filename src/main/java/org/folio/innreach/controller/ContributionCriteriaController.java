package org.folio.innreach.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.dto.ContributionCriteriaConfigurationDTO;
import org.folio.innreach.domain.dto.ContributionCriteriaExcludedLocationDTO;
import org.folio.innreach.domain.dto.ContributionCriteriaStatisticalCodeBehaviorDTO;
import org.folio.innreach.domain.entity.ContributionBehavior;
import org.folio.innreach.domain.service.ContributionCriteriaConfigurationService;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.folio.innreach.dto.InnReachLocationDTO;
import org.folio.innreach.dto.Metadata;
import org.folio.innreach.mapper.DateMapper;
import org.folio.innreach.rest.resource.ContributionCriteriaApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping("/inn-reach/central-servers/")
public class ContributionCriteriaController implements ContributionCriteriaApi {

  private final ContributionCriteriaConfigurationService criteriaConfigurationService;
  private final DateMapper dateMapper;

  @PostMapping("/contribution-criteria")
  public ResponseEntity<ContributionCriteriaDTO> postContributionCriteria(@Valid @RequestBody ContributionCriteriaDTO contributionCriteriaDTO) {
    return ResponseEntity.status(HttpStatus.CREATED).body(toContributionCriteriaDTO(criteriaConfigurationService.create(toContributionCriteriaConfigurationDTO(contributionCriteriaDTO))));
  }


  @Override
  @DeleteMapping("/{centralServerId}/contribution-criteria")
  public ResponseEntity<Void> deleteCriteria(@PathVariable UUID centralServerId) {
    criteriaConfigurationService.delete(centralServerId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{centralServerId}/contribution-criteria")
  public ResponseEntity<ContributionCriteriaDTO> getCriteriaById(@PathVariable UUID centralServerId) {
    return ResponseEntity.ok(
      toContributionCriteriaDTO(
        criteriaConfigurationService.get(centralServerId)
      )
    );
  }

  @PutMapping("/{centralServerId}/contribution-criteria")
  public ResponseEntity<ContributionCriteriaDTO> updateCriteria(@PathVariable UUID centralServerId,
                                                            @Valid ContributionCriteriaDTO contributionCriteriaDTO) {
    return ResponseEntity.ok(toContributionCriteriaDTO(
      criteriaConfigurationService.update(toContributionCriteriaConfigurationDTO(contributionCriteriaDTO))
    ));
  }

  public ContributionCriteriaDTO toContributionCriteriaDTO (ContributionCriteriaConfigurationDTO contributionCriteriaConfigurationDTO) {
    var result = new ContributionCriteriaDTO();
    result.setCentralServerId(contributionCriteriaConfigurationDTO.getCentralServerId());
    var metadata = new Metadata();
    metadata.setCreatedByUserId(contributionCriteriaConfigurationDTO.getCreatedBy());
    metadata.setUpdatedByUserId(contributionCriteriaConfigurationDTO.getLastModifiedBy());
    metadata.setCreatedDate(dateMapper.offsetDateTimeAsDate(contributionCriteriaConfigurationDTO.getCreatedDate()));
    metadata.setUpdatedDate(dateMapper.offsetDateTimeAsDate(contributionCriteriaConfigurationDTO.getLastModifiedDate()));
    result.setMetadata(metadata);
    contributionCriteriaConfigurationDTO.getExcludedLocations().stream().forEach(excludedLocationDTO -> result.addLocationIdsItem(excludedLocationDTO.getExcludedLocationId()));
    contributionCriteriaConfigurationDTO.getStatisticalCodeBehaviors().stream().forEach(statisticalCodeBehaviorDTO -> {
      switch (statisticalCodeBehaviorDTO.getContributionBehavior()) {
        case doNotContribute:
          result.setDoNotContributeId(statisticalCodeBehaviorDTO.getStatisticalCodeId());
          break;
        case contributeButSuppress:
          result.setContributeButSuppressId(statisticalCodeBehaviorDTO.getStatisticalCodeId());
          break;
        case contributeAsSystemOwned:
          result.setContributeAsSystemOwnedId(statisticalCodeBehaviorDTO.getStatisticalCodeId());
      }
    });
    return  result;
  }

  public ContributionCriteriaConfigurationDTO toContributionCriteriaConfigurationDTO(ContributionCriteriaDTO contributionCriteriaDTO) {
    var result = new ContributionCriteriaConfigurationDTO();
    result.setCentralServerId(contributionCriteriaDTO.getCentralServerId());
    result.setExcludedLocations(new HashSet<>());
    contributionCriteriaDTO.getLocationIds().stream().forEach(locationId -> {
      var excludedLocationDTO = new ContributionCriteriaExcludedLocationDTO();
      excludedLocationDTO.setExcludedLocationId(locationId);
      result.getExcludedLocations().add(excludedLocationDTO);
    });
    result.setStatisticalCodeBehaviors(new HashSet<>());

    var statisticalCodeBehaviorDTO = new ContributionCriteriaStatisticalCodeBehaviorDTO();
    if (contributionCriteriaDTO.getContributeButSuppressId()!=null) {
      statisticalCodeBehaviorDTO.setContributionBehavior(ContributionBehavior.contributeButSuppress);
      statisticalCodeBehaviorDTO.setStatisticalCodeId(contributionCriteriaDTO.getContributeButSuppressId());
      result.getStatisticalCodeBehaviors().add(statisticalCodeBehaviorDTO);
    }

    if (contributionCriteriaDTO.getDoNotContributeId()!=null) {
      statisticalCodeBehaviorDTO = new ContributionCriteriaStatisticalCodeBehaviorDTO();
      statisticalCodeBehaviorDTO.setContributionBehavior(ContributionBehavior.doNotContribute);
      statisticalCodeBehaviorDTO.setStatisticalCodeId(contributionCriteriaDTO.getDoNotContributeId());
      result.getStatisticalCodeBehaviors().add(statisticalCodeBehaviorDTO);
    }

    if (contributionCriteriaDTO.getContributeAsSystemOwnedId()!=null) {
      statisticalCodeBehaviorDTO = new ContributionCriteriaStatisticalCodeBehaviorDTO();
      statisticalCodeBehaviorDTO.setContributionBehavior(ContributionBehavior.contributeAsSystemOwned);
      statisticalCodeBehaviorDTO.setStatisticalCodeId(contributionCriteriaDTO.getContributeAsSystemOwnedId());
      result.getStatisticalCodeBehaviors().add(statisticalCodeBehaviorDTO);
    }
    return result;
  }



  private ContributionCriteriaDTO stubContributionCriteriaDTO(UUID centralServerId) {
    var res = new ContributionCriteriaDTO();
    res.setCentralServerId(centralServerId);
    res.setContributeAsSystemOwnedId(UUID.randomUUID());
    res.setContributeButSuppressId(UUID.randomUUID());
    res.setDoNotContributeId(UUID.randomUUID());
    res.setMetadata(stubMetaData());
    res.addLocationIdsItem(UUID.randomUUID());
    res.addLocationIdsItem(UUID.randomUUID());
    res.addLocationIdsItem(UUID.randomUUID());
    return res;
  }

  private Metadata stubMetaData() {
    var metaData = new Metadata();
    metaData.setCreatedByUserId(UUID.randomUUID().toString());
    metaData.setUpdatedByUserId(UUID.randomUUID().toString());
    metaData.setCreatedDate(new Date());
    metaData.setUpdatedDate(new Date());
    return metaData;
  }
}
