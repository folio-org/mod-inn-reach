package org.folio.innreach.fixture;

import static org.jeasy.random.FieldPredicates.named;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.RandomStringUtils;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus;
import org.folio.innreach.domain.entity.base.AuditableUser;
import org.folio.innreach.dto.Item;

@UtilityClass
public class InventoryItemFixture {

  private static final EasyRandom itemRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("hrid"), () -> RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT))
      .randomize(named("createdBy"), () -> AuditableUser.SYSTEM)
      .randomize(named("createdDate"), OffsetDateTime::now)
      .excludeField(named("contribution"))
      .excludeField(named("updatedBy"))
      .excludeField(named("updatedDate"))
      .excludeField(named("metadata"));

    itemRandom = new EasyRandom(params);
  }

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

  public static InventoryItemDTO createInventoryItemDTO(){
    return itemRandom.nextObject(InventoryItemDTO.class);
  }

  public static Item createInventoryItem(){
    return itemRandom.nextObject(Item.class);
  }
}
