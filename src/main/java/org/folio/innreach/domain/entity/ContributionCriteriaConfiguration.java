package org.folio.innreach.domain.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

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

}
