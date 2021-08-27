package org.folio.innreach.domain.dto.folio.inventorystorage;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MaterialTypeDTO {

  private UUID id;
  private String name;
  private String source;

}
