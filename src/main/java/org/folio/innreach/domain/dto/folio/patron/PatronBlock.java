package org.folio.innreach.domain.dto.folio.patron;

import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class PatronBlock {
  private UUID patronBlockConditionId;
  private Boolean blockBorrowing;
  private Boolean blockRenewals;
  private Boolean blockRequests;
  private String message;
}
