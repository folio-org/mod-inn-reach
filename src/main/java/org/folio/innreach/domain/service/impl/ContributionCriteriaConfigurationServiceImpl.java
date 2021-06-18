package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.dto.ContributionCriteriaConfigurationDTO;
import org.folio.innreach.domain.dto.ContributionCriteriaExcludedLocationDTO;
import org.folio.innreach.domain.dto.ContributionCriteriaStatisticalCodeBehaviorDTO;
import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.folio.innreach.domain.entity.ContributionCriteriaExcludedLocation;
import org.folio.innreach.domain.entity.ContributionCriteriaStatisticalCodeBehavior;

import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.ContributionCriteriaConfigurationService;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.folio.innreach.mapper.ContributionCriteriaConfigurationMapper;
import org.folio.innreach.mapper.ContributionCriteriaExcludedLocationMapper;
import org.folio.innreach.mapper.ContributionCriteriaMapper;
import org.folio.innreach.mapper.ContributionCriteriaMapperImpl;
import org.folio.innreach.mapper.ContributionCriteriaStatisticalCodeBehaviorMapper;
import org.folio.innreach.repository.ContributionCriteriaConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityExistsException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@Service
public class ContributionCriteriaConfigurationServiceImpl implements ContributionCriteriaConfigurationService {
  private static final String TEXT_CONTRIBUTION_CRITERIA_CONFIGURATION_WITH_ID
    = "Contribution Criteria Configuration with id: ";
  private final ContributionCriteriaConfigurationRepository contributionCriteriaConfigurationRepository;
  private final ContributionCriteriaConfigurationMapper contributionCriteriaConfigurationMapper;
  private final ContributionCriteriaExcludedLocationMapper excludedLocationMapper;
  private final ContributionCriteriaStatisticalCodeBehaviorMapper statisticalCodeMapper;
  private final ContributionCriteriaMapper contributionCriteriaMapper;

  @Override
  public ContributionCriteriaDTO create(ContributionCriteriaDTO contributionCriteriaDTO) {
    var criteriaConfigurationDTO = contributionCriteriaMapper.toContributionCriteriaConfigurationDTO(contributionCriteriaDTO);
    var centralServerId = criteriaConfigurationDTO.getCentralServerId();
    contributionCriteriaConfigurationRepository.findById(centralServerId).ifPresent(
      criteriaConfiguration -> {
        throw new EntityExistsException(TEXT_CONTRIBUTION_CRITERIA_CONFIGURATION_WITH_ID
          + criteriaConfiguration.getCentralServerId() + " already exists.");
      });
    var contributionCriteriaConfiguration
      = contributionCriteriaConfigurationMapper.toEntity(criteriaConfigurationDTO);
    contributionCriteriaConfiguration.getStatisticalCodeBehaviors().forEach(
      statisticalCodeBehavior -> statisticalCodeBehavior.setContributionCriteriaConfiguration(contributionCriteriaConfiguration));
    contributionCriteriaConfiguration.getExcludedLocations().forEach(
      excludedLocation -> excludedLocation.setContributionCriteriaConfiguration(contributionCriteriaConfiguration));
    return contributionCriteriaMapper.toContributionCriteriaDTO(contributionCriteriaConfigurationMapper.toDto(
      contributionCriteriaConfigurationRepository
        .save(contributionCriteriaConfiguration)
    ));
  }

  @Override
  @Transactional(readOnly = true)
  public ContributionCriteriaDTO get(UUID centralServerId) {
    var entity = contributionCriteriaConfigurationRepository.findById(centralServerId).orElseThrow(
      () -> new EntityNotFoundException("Configuration for central server id: " + centralServerId + " not found"));
    return contributionCriteriaMapper.toContributionCriteriaDTO(
      contributionCriteriaConfigurationMapper.toDto(entity)
    );
  }

  @Override
  @Transactional
  public ContributionCriteriaDTO update(ContributionCriteriaDTO contributionCriteriaDTO) {
    var criteriaConfigurationDTO
      = contributionCriteriaMapper.toContributionCriteriaConfigurationDTO(contributionCriteriaDTO);
    var criteriaConfigurationForUpdate
      = contributionCriteriaConfigurationRepository.findById(criteriaConfigurationDTO.getCentralServerId())
      .orElseThrow(() -> new EntityNotFoundException("Configuration for Central Server id: "
        + criteriaConfigurationDTO.getCentralServerId() + "not found."));

    updateExcludedLocations(criteriaConfigurationDTO.getExcludedLocations(), criteriaConfigurationForUpdate);
    updateStatisticalCodeBehaviors(criteriaConfigurationDTO.getStatisticalCodeBehaviors(), criteriaConfigurationForUpdate);
    return contributionCriteriaMapper.toContributionCriteriaDTO(contributionCriteriaConfigurationMapper.toDto(
      contributionCriteriaConfigurationRepository.save(criteriaConfigurationForUpdate))
    );
  }

  public void updateStatisticalCodeBehaviors(Set<ContributionCriteriaStatisticalCodeBehaviorDTO> newStatisticalCodeBehaviorDTOS,
                                             ContributionCriteriaConfiguration criteriaConfigurationForUpdate) {
    if (newStatisticalCodeBehaviorDTOS.isEmpty()) {
      Set<ContributionCriteriaStatisticalCodeBehavior> statisticalCodeBehaviorForRemove
        = new HashSet<>(criteriaConfigurationForUpdate.getStatisticalCodeBehaviors());
      statisticalCodeBehaviorForRemove.forEach(criteriaConfigurationForUpdate::removeStatisticalCondeBehavior);
    } else {
      Set<ContributionCriteriaStatisticalCodeBehaviorDTO> statisticalCodeBehaviorForRemove = new HashSet<>();
      Set<ContributionCriteriaStatisticalCodeBehaviorDTO> statisticalCodeBehaviorForAdd
        = new HashSet<>(newStatisticalCodeBehaviorDTOS);
      criteriaConfigurationForUpdate.getStatisticalCodeBehaviors().forEach(statisticalCodeBehavior ->
        statisticalCodeBehaviorForRemove.add(statisticalCodeMapper.toDTO(statisticalCodeBehavior))
      );
      statisticalCodeBehaviorForAdd.removeAll(statisticalCodeBehaviorForRemove);
      statisticalCodeBehaviorForRemove.removeAll(newStatisticalCodeBehaviorDTOS);

      Map<String, ContributionCriteriaStatisticalCodeBehavior> itemsForUpdate = new HashMap<>();
      criteriaConfigurationForUpdate.getStatisticalCodeBehaviors().forEach(statisticalCodeBehavior ->
        itemsForUpdate.put(statisticalCodeBehavior.getStatisticalCodeId().toString()
          + statisticalCodeBehavior.getContributionBehavior(), statisticalCodeBehavior)
      );
      statisticalCodeBehaviorForRemove.forEach(statisticalCodeBehaviorDTO -> {
        ContributionCriteriaStatisticalCodeBehavior statisticalCodeBehavior
          = itemsForUpdate.get(statisticalCodeBehaviorDTO.getStatisticalCodeId().toString()
          + statisticalCodeBehaviorDTO.getContributionBehavior());
        if (statisticalCodeBehavior != null)
          criteriaConfigurationForUpdate.removeStatisticalCondeBehavior(statisticalCodeBehavior);
      });
      statisticalCodeBehaviorForAdd.forEach(statisticalCodeBehaviorDTO ->
        criteriaConfigurationForUpdate.addStatisticalCodeBehavior(
          statisticalCodeMapper.toEntity(statisticalCodeBehaviorDTO))
      );
    }
  }

  public void updateExcludedLocations(Set<ContributionCriteriaExcludedLocationDTO> newExcludedLocationDTOS,
                                      ContributionCriteriaConfiguration criteriaConfigurationForUpdate) {
    if (newExcludedLocationDTOS.isEmpty()) {
      Set<ContributionCriteriaExcludedLocation> excludedLocationsForRemove = new HashSet<>(criteriaConfigurationForUpdate.getExcludedLocations());
      excludedLocationsForRemove.forEach(criteriaConfigurationForUpdate::removeExcludedLocation);
    } else {
      Set<ContributionCriteriaExcludedLocationDTO> locationsForRemove = new HashSet<>();
      Set<ContributionCriteriaExcludedLocationDTO> locationsForAdd = new HashSet<>(newExcludedLocationDTOS);
      criteriaConfigurationForUpdate.getExcludedLocations().forEach(excludedLocation ->
        locationsForRemove.add(excludedLocationMapper.toDTO(excludedLocation))
      );
      locationsForAdd.removeAll(locationsForRemove);
      locationsForRemove.removeAll(newExcludedLocationDTOS);

      Map<UUID, ContributionCriteriaExcludedLocation> itemsForUpdate = new HashMap<>();
      criteriaConfigurationForUpdate.getExcludedLocations().forEach(excludedLocation ->
        itemsForUpdate.put(excludedLocation.getExcludedLocationId(), excludedLocation)
      );
      locationsForRemove.forEach(excludedLocationDTO -> {
        ContributionCriteriaExcludedLocation excludedLocation = itemsForUpdate.get(excludedLocationDTO.getExcludedLocationId());
        if (excludedLocation != null)
          criteriaConfigurationForUpdate.removeExcludedLocation(excludedLocation);
      });
      locationsForAdd.forEach(excludedLocationDTO ->
        criteriaConfigurationForUpdate.addExcludedLocation(excludedLocationMapper.toEntity(excludedLocationDTO))
      );
    }
  }

  @Override
  @Transactional
  public void delete(UUID centralServerId) {
    var criteriaConfiguration
      = contributionCriteriaConfigurationRepository.findById(centralServerId).orElseThrow(() -> new EntityNotFoundException(TEXT_CONTRIBUTION_CRITERIA_CONFIGURATION_WITH_ID + centralServerId + " not found!"));
    contributionCriteriaConfigurationRepository.delete(criteriaConfiguration);
  }
}
