package org.folio.innreach.domain.dto.folio.requestpreference;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestPreferenceDTO {
  private UUID userId;
  private UUID defaultServicePointId;
}
