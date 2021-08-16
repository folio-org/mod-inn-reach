package org.folio.innreach.domain.dto.folio.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.dto.FieldConfigurationDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdentifierWithConfigDTO {

  private InventoryInstanceDTO.IdentifierDTO identifierDTO;
  private FieldConfigurationDTO fieldConfigurationDTO;
}
