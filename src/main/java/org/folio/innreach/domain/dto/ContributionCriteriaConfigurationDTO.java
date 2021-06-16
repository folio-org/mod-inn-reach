package org.folio.innreach.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.folio.innreach.dto.Metadata;

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
  private UUID centralServerId;
  private Set<ContributionCriteriaExcludedLocationDTO> excludedLocations;
  private Set<ContributionCriteriaStatisticalCodeBehaviorDTO> statisticalCodeBehaviors;

  private Metadata metadata;
}
