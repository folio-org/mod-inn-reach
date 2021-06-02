package org.folio.innreach.domain.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.folio.innreach.domain.entity.ContributionBehavior;
import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;

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




@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"id","contributionCriteriaConfiguration"})
public class ContributionCriteriaStatisticalCodeBehaviorDTO {
  private UUID id;

  private UUID statisticalCodeId;

  private ContributionBehavior contributionBehavior;

  private ContributionCriteriaConfiguration contributionCriteriaConfiguration;
}
