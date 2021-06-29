package org.folio.innreach.domain.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter

@EqualsAndHashCode(of = {"excludedLocationId"})
public class ContributionCriteriaExcludedLocationDTO {
  private UUID id;
  private UUID excludedLocationId;
}
