package org.folio.innreach.domain.dto.folio.circulation;

import java.util.UUID;

import lombok.Value;

@Value(staticConstructor = "of")
public class RenewByIdDTO {

  UUID itemId;
  UUID userId;

}