package org.folio.innreach.domain.dto.folio.inventorystorage;

import java.util.UUID;

import lombok.Data;

@Data
public class LocationDTO {
  private UUID id;
  private UUID libraryId;
}
