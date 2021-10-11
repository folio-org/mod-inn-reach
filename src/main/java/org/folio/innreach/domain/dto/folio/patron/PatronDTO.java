package org.folio.innreach.domain.dto.folio.patron;

import static java.util.Collections.emptyList;

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
  private Integer totalLoans = 0;
  private List<Loan> loans = emptyList();

  @Data
  @ToString
  @NoArgsConstructor
  @AllArgsConstructor(staticName = "of")
  public static class Loan {
    private UUID id;
  }
}
