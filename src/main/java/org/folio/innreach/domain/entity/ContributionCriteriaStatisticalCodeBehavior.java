package org.folio.innreach.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "statistical_code_behavior")

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"statistical_code_id", "contributionBehavior", "contributionCriteriaConfiguration"})
public class ContributionCriteriaStatisticalCodeBehavior {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "statistical_code_id")
  private UUID statisticalCodeId;

  @Enumerated(EnumType.STRING)
  private ContributionBehavior contributionBehavior;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "criteria_configuration_id")
  private ContributionCriteriaConfiguration contributionCriteriaConfiguration;

  public static enum ContributionBehavior {
    contributeButSuppress,
    contributeAsSystemOwned,
    doNotContribute
  }
}
