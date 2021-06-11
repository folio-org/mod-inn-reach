package org.folio.innreach.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {})
@Builder
@Getter
@Setter
public class ContributionCriteriaConfigurationDTO {
  UUID centralServerId;
  Set<ContributionCriteriaExcludedLocationDTO> excludedLocations;
  Set<ContributionCriteriaStatisticalCodeBehaviorDTO> statisticalCodeBehaviors;

  private String createdBy;
  private OffsetDateTime createdDate;
  private String lastModifiedBy;
  private OffsetDateTime lastModifiedDate;
}
