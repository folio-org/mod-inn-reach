package org.folio.innreach.domain.dto.folio.circulation;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MoveRequestDTO {
  private UUID destinationItemId;
  private String requestType;
}
