package org.folio.innreach.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.hibernate.annotations.QueryHints;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;
import javax.persistence.Table;
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

@Entity
@Table(name = "contribution_criteria_configuration")
public class ContributionCriteriaConfiguration {

  @Id
  @Column(name = "central_server_id")
  UUID centralServeId;

  @OneToMany(
    fetch = FetchType.LAZY,
    cascade = CascadeType.ALL,
    mappedBy = "contributionCriteriaConfiguration",
    orphanRemoval = true
  )
  Set<ContributionCriteriaExcludedLocation> excludedLocations = new HashSet<>();

  @OneToMany(
    fetch = FetchType.LAZY,
    cascade = CascadeType.ALL,
    mappedBy = "contributionCriteriaConfiguration",
    orphanRemoval = true
  )
  Set<ContributionCriteriaStatisticalCodeBehavior> statisticalCodeBehaviors = new HashSet<>();

  public void addStatisticalCodeBehavior(ContributionCriteriaStatisticalCodeBehavior statisticalCodeBehavior) {
    statisticalCodeBehavior.setContributionCriteriaConfiguration(this);
    statisticalCodeBehaviors.add(statisticalCodeBehavior);
  }

  public void removeStatisticalCondeBehavior(UUID statisticalCodeId) {
    statisticalCodeBehaviors.removeAll(getStatisticalCodeBehaviors().stream()
      .filter(statisticalCodeBehavior -> statisticalCodeBehavior.getStatisticalCodeId().equals(statisticalCodeId))
      .collect(Collectors.toSet()));
  }

  public void addExcludedLocationId(UUID locationId) {
    if (excludedLocations.stream().filter(excludedLocation -> excludedLocation.getExcludedLocationId().equals(locationId)).findAny().isEmpty()) {
      ContributionCriteriaExcludedLocation locationForAdd = new ContributionCriteriaExcludedLocation();
      locationForAdd.setExcludedLocationId(locationId);
      locationForAdd.setContributionCriteriaConfiguration(this);
      excludedLocations.add(locationForAdd);
    } else {
      log.warn("Excluded location already exist. UUID: " + locationId);
    }
  }
  public void removeExcludedLocationId(UUID locationId) {
    excludedLocations.removeAll(getExcludedLocations().stream()
      .filter(excludedLocation -> excludedLocation.getExcludedLocationId().equals(locationId))
      .collect(Collectors.toSet()));
  }
}
