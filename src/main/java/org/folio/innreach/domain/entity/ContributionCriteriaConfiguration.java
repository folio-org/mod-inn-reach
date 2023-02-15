package org.folio.innreach.domain.entity;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.folio.innreach.domain.entity.base.AbstractEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "contribution_criteria_configuration")
public class ContributionCriteriaConfiguration extends AbstractEntity {

  private UUID contributeButSuppressCodeId;
  private UUID contributeAsSystemOwnedCodeId;
  private UUID doNotContributeCodeId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id", unique = true)
  private CentralServer centralServer;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "contribution_criteria_excluded_location",
      joinColumns = @JoinColumn(name = "contribution_criteria_id")
  )
  @Column(name = "location_id")
  @org.hibernate.annotations.Fetch(value = org.hibernate.annotations.FetchMode.SUBSELECT)
  private List<UUID> excludedLocationIds = new ArrayList<>();


  public ContributionCriteriaConfiguration(UUID id) {
    super(id);
  }

  public void setExcludedLocationIds(List<UUID> excludedLocationIds) {
    if (isEmpty(excludedLocationIds)) {
      this.excludedLocationIds = new ArrayList<>();
    } else {
      this.excludedLocationIds = excludedLocationIds;
    }
  }

  public void addExcludedLocationId(UUID id) {
    Objects.requireNonNull(id);

    excludedLocationIds.add(id);
  }

  public void removeExcludedLocationId(UUID id) {
    Objects.requireNonNull(id);

    excludedLocationIds.remove(id);
  }

}
