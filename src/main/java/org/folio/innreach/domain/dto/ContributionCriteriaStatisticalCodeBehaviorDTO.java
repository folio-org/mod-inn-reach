package org.folio.innreach.domain.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.folio.innreach.domain.entity.ContributionBehavior;
import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;

import java.util.UUID;




@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = {"statisticalCodeId","contributionBehavior"})
public class ContributionCriteriaStatisticalCodeBehaviorDTO {
  private UUID id;

  private UUID statisticalCodeId;

  private ContributionBehavior contributionBehavior;

  private ContributionCriteriaConfiguration contributionCriteriaConfiguration;
}
