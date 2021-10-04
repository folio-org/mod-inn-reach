package org.folio.innreach.domain.dto.folio.patron;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class PatronDTO {
  private Integer totalLoans;
  private List<Loan> loans;

  @Data
  @ToString
  @NoArgsConstructor
  @AllArgsConstructor(staticName = "of")
  public static class Loan {
    private UUID id;
  }
}
