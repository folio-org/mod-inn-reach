package org.folio.innreach.domain.dto.folio.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryInstanceDTO {

  private UUID id;
  private String title;
  private List<IdentifierDTO> identifiers;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class IdentifierDTO {

    @JsonProperty("identifierTypeId")
    private UUID id;
    private String value;
  }
}
