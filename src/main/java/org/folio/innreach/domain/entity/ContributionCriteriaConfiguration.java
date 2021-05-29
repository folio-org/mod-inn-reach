package org.folio.innreach.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.entity.base.Auditable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"createdBy","createdDate","lastModifiedBy","lastModifiedDate","excludedLocations","updateCounter"})

@Entity
@Table(name = "contribution_criteria_configuration")
public class ContributionCriteriaConfiguration extends Auditable<String> {
  @Id
  @Column(name = "central_server_id")
  private UUID centralServeId;

  @OneToMany(
    fetch = FetchType.LAZY,
    cascade = CascadeType.ALL,
    mappedBy = "contributionCriteriaConfiguration",
    orphanRemoval = true
  )
  private Set<ContributionCriteriaExcludedLocation> excludedLocations = new HashSet<>();

  @OneToMany(
    fetch = FetchType.LAZY,
    cascade = CascadeType.ALL,
    mappedBy = "contributionCriteriaConfiguration",
    orphanRemoval = true
  )
  private Set<ContributionCriteriaStatisticalCodeBehavior> statisticalCodeBehaviors = new HashSet<>();

  private Integer updateCounter = 0;

  @Transactional
  public void updateExcludedLocations(List<ContributionCriteriaExcludedLocation> obtainedNewExcludedLocations) {
    var excludedLocationsHashCodeBeforeUpdate = getExcludedLocations().hashCode();
    if (obtainedNewExcludedLocations.size() == 0) getExcludedLocations().clear();
    else {
      List<ContributionCriteriaExcludedLocation> locationsForDelete = new ArrayList<>(getExcludedLocations());
      locationsForDelete.removeAll(obtainedNewExcludedLocations);

      List<ContributionCriteriaExcludedLocation> locationsForAdd = new ArrayList<>(obtainedNewExcludedLocations);
      locationsForAdd.removeAll(getExcludedLocations());

      locationsForAdd.stream().forEach(excludedLocation -> {
        excludedLocation.setContributionCriteriaConfiguration(this);
      });

      getExcludedLocations().removeAll(locationsForDelete);
      getExcludedLocations().addAll(locationsForAdd);
    }
    if (excludedLocationsHashCodeBeforeUpdate != getExcludedLocations().hashCode()) touchUpdateTrigger();
  }

  public void addStatisticalCodeBehavior(@NotNull ContributionCriteriaStatisticalCodeBehavior statisticalCodeBehavior) {
    int hashCodeBeforeUpdate = statisticalCodeBehaviors.hashCode();
    statisticalCodeBehavior.setContributionCriteriaConfiguration(this);
    statisticalCodeBehaviors.add(statisticalCodeBehavior);
    if (hashCodeBeforeUpdate != statisticalCodeBehaviors.hashCode()) touchUpdateTrigger();
  }

  public void removeStatisticalCondeBehavior(@NotNull UUID statisticalCodeId) {
    int hashCodeBeforeUpdate = statisticalCodeBehaviors.hashCode();
    statisticalCodeBehaviors.removeAll(getStatisticalCodeBehaviors().stream()
      .filter(statisticalCodeBehavior -> statisticalCodeBehavior.getStatisticalCodeId().equals(statisticalCodeId))
      .collect(Collectors.toSet()));
    if (hashCodeBeforeUpdate != statisticalCodeBehaviors.hashCode()) touchUpdateTrigger();
  }

  public void addExcludedLocationId(@NotNull UUID locationId) {
    if (excludedLocations.stream().filter(excludedLocation -> excludedLocation.getExcludedLocationId().equals(locationId)).findAny().isEmpty()) {
      int hashCodeBeforeUpdate = excludedLocations.hashCode();
      ContributionCriteriaExcludedLocation locationForAdd = new ContributionCriteriaExcludedLocation();
      locationForAdd.setExcludedLocationId(locationId);
      locationForAdd.setContributionCriteriaConfiguration(this);
      excludedLocations.add(locationForAdd);
      if (hashCodeBeforeUpdate != excludedLocations.hashCode()) touchUpdateTrigger();
    } else {
      log.warn("Excluded location already exist. UUID: " + locationId);
    }
  }

  public void removeExcludedLocationId(UUID locationId) {
    int hashCodeBeforeUpdate = excludedLocations.hashCode();
    excludedLocations.removeAll(getExcludedLocations().stream()
      .filter(excludedLocation -> excludedLocation.getExcludedLocationId().equals(locationId))
      .collect(Collectors.toSet()));
    if (hashCodeBeforeUpdate != excludedLocations.hashCode()) touchUpdateTrigger();
  }

  private void touchUpdateTrigger() {
    if (updateCounter < Integer.MAX_VALUE) {
      updateCounter++;
    } else {
      updateCounter = Integer.MIN_VALUE;
    }
  }
}
