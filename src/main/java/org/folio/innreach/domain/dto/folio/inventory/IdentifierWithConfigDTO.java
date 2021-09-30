package org.folio.innreach.domain.dto.folio.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.folio.innreach.dto.FieldConfigurationDTO;
import org.folio.innreach.dto.InstanceIdentifiers;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdentifierWithConfigDTO {

  private InstanceIdentifiers identifierDTO;
  private FieldConfigurationDTO fieldConfigurationDTO;
}
