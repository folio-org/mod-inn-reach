package org.folio.innreach.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.entity.base.Auditable;
import org.hibernate.annotations.QueryHints;
import org.joda.time.LocalDateTime;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString

@Entity
@Table(name = "contribution_criteria_configuration")
public class ContributionCriteriaConfiguration extends Auditable<String> {
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

  Integer updateCounter = 0;

  public void addStatisticalCodeBehavior(ContributionCriteriaStatisticalCodeBehavior statisticalCodeBehavior) {
    int hashCodeBeforeUpdate = statisticalCodeBehaviors.hashCode();
    statisticalCodeBehavior.setContributionCriteriaConfiguration(this);
    statisticalCodeBehaviors.add(statisticalCodeBehavior);
    if (hashCodeBeforeUpdate != statisticalCodeBehaviors.hashCode()) touchUpdateTrigger();
  }

  public void removeStatisticalCondeBehavior(UUID statisticalCodeId) {
    int hashCodeBeforeUpdate = statisticalCodeBehaviors.hashCode();
    statisticalCodeBehaviors.removeAll(getStatisticalCodeBehaviors().stream()
      .filter(statisticalCodeBehavior -> statisticalCodeBehavior.getStatisticalCodeId().equals(statisticalCodeId))
      .collect(Collectors.toSet()));
    if (hashCodeBeforeUpdate != statisticalCodeBehaviors.hashCode()) touchUpdateTrigger();
  }

  public void addExcludedLocationId(UUID locationId) {
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
