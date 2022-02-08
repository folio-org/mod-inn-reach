package org.folio.innreach.domain.dto.folio.inventorystorage;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationDTO {
  private UUID id;
  private UUID libraryId;
}
