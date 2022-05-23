package org.folio.innreach.domain.dto.folio.inventorystorage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicePointUserDTO {
  private UUID userId;
  private UUID defaultServicePointId;
}
