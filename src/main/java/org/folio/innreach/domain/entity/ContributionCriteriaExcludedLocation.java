package org.folio.innreach.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "contribution_criteria_excluded_location")

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
//@EqualsAndHashCode(exclude = {"contributionCriteriaConfiguration","excluded_location_id"})
@EqualsAndHashCode(exclude = {"contributionCriteriaConfiguration","id"})
public class ContributionCriteriaExcludedLocation {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "excluded_location_id")
  private UUID excludedLocationId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "criteria_configuration_id")
  private ContributionCriteriaConfiguration contributionCriteriaConfiguration;
}
