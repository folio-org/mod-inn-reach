package org.folio.innreach.fixture;

import java.util.UUID;

import lombok.experimental.UtilityClass;

import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus;

@UtilityClass
public class InventoryItemFixture {

  public static InventoryItemDTO createInventoryItemDTO(InventoryItemStatus status, UUID materialTypeId,
      UUID permanentLoanTypeId, UUID temporaryLoanTypeId, UUID permanentLocationId) {
    return InventoryItemDTO.builder()
      .id(UUID.randomUUID())
      .status(status)
      .materialType(new InventoryItemDTO.MaterialType(materialTypeId, "materialType"))
      .permanentLoanType(new InventoryItemDTO.PermanentLoanType(permanentLoanTypeId, "permanentLoanType"))
      .temporaryLoanType(new InventoryItemDTO.TemporaryLoanType(temporaryLoanTypeId, "temporaryLoanType"))
      .permanentLocation(new InventoryItemDTO.PermanentLocation(permanentLocationId, "permanentLocation"))
      .build();
  }
}
