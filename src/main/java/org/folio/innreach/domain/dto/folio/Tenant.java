package org.folio.innreach.domain.dto.folio;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Tenant {
  private String id;
  private String name;
  private String description;
}
