package org.folio.innreach.domain.dto.folio;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContributionItemCirculationStatus {

  AVAILABLE("Available"),
  NOT_AVAILABLE("Not Available"),
  ON_LOAN("On Loan"),
  NON_LENDABLE("Non-Lendable");

  private final String status;

  public static ContributionItemCirculationStatus fromStatus(String status) {
    return Arrays.stream(values())
      .filter(itemStatus -> itemStatus.status.equalsIgnoreCase(status))
      .findFirst()
      .orElse(null);
  }
}
