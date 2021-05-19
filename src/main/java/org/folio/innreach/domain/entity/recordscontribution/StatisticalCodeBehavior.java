package org.folio.innreach.domain.entity.recordscontribution;

import lombok.Data;
import lombok.NoArgsConstructor;
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
import javax.persistence.Transient;
import java.util.UUID;

@Entity
@Table(name = "statistical_code_behavior")

@NoArgsConstructor
@Data
public class StatisticalCodeBehavior {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  UUID id;

  @Column(name = "statistical_code_id")
  UUID statisticalCodeId;

  @Column(name = "contribution_behavior")
  @Enumerated(EnumType.STRING)
  ContributionBehavior contributionBehavior;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contribution_criteria_configuration_id")
  private ContributionCriteriaConfiguration centralServer;

  public enum ContributionBehavior {
    contributeButSuppress,
    contributeAsSystemOwned,
    doNotContribute
  }

  @Transient
  private final String statCodeSuffix = "StatisticalCodeId";
}
