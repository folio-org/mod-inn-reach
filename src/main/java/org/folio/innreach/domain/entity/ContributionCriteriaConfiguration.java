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
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Log4j2
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate", "excludedLocations", "updateCounter", "statisticalCodeBehaviors"})

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

  public void addExcludedLocation(ContributionCriteriaExcludedLocation excludedLocationForAdd) {
    int hashCodeBeforeUpdate = excludedLocations.hashCode();
    excludedLocationForAdd.setContributionCriteriaConfiguration(this);
    excludedLocations.add(excludedLocationForAdd);
    if (hashCodeBeforeUpdate != excludedLocations.hashCode()) touchUpdateTrigger();
  }

  public void removeExcludedLocation(ContributionCriteriaExcludedLocation excludedLocationForRemove) {
    int hashCodeBeforeUpdate = excludedLocations.hashCode();
    excludedLocations.remove(excludedLocationForRemove);
    excludedLocationForRemove.setContributionCriteriaConfiguration(null);
    if (hashCodeBeforeUpdate != excludedLocations.hashCode()) touchUpdateTrigger();
  }

  public void addStatisticalCodeBehavior(ContributionCriteriaStatisticalCodeBehavior statisticalCodeBehavior) {
    int hashCodeBeforeUpdate = statisticalCodeBehaviors.hashCode();
    statisticalCodeBehavior.setContributionCriteriaConfiguration(this);
    statisticalCodeBehaviors.add(statisticalCodeBehavior);
    if (hashCodeBeforeUpdate != statisticalCodeBehaviors.hashCode()) touchUpdateTrigger();
  }

  public void removeStatisticalCondeBehavior(ContributionCriteriaStatisticalCodeBehavior statisticalCodeBehaviorForRemove) {
    int hashCodeBeforeUpdate = statisticalCodeBehaviors.hashCode();
    statisticalCodeBehaviors.remove(statisticalCodeBehaviorForRemove);
    statisticalCodeBehaviorForRemove.setContributionCriteriaConfiguration(null);
    if (hashCodeBeforeUpdate != statisticalCodeBehaviors.hashCode()) touchUpdateTrigger();
  }

  private void touchUpdateTrigger() {
    if (updateCounter < Integer.MAX_VALUE) {
      updateCounter++;
    } else {
      updateCounter = Integer.MIN_VALUE;
    }
  }
}
