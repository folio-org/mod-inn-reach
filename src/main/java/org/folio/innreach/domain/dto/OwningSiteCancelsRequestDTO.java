package org.folio.innreach.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwningSiteCancelsRequestDTO {
  private String localBibId;
  private String reason;
  private Integer reasonCode;
  private String patronName;
}
