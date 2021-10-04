package org.folio.innreach.domain.dto.folio.patron;

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
}
