package org.folio.innreach.domain.dto.folio.inventory;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InventoryItemStatus {

  AVAILABLE("Available"),
  RECENTLY_RETURNED("Recently Returned"),
  IN_TRANSIT("In transit"),
  CHECKED_OUT("Checked out"),
  PAGED("Paged"),
  DECLARED_LOST("Declared lost"),
  WITHDRAWN("Withdrawn"),
  AWAITING_PICKUP("Awaiting pickup"),
  AGED_TO_LOST("Aged to lost"),
  LONG_MISSING("Long missing"),
  AWAITING_DELIVERY("Awaiting delivery"),
  MISSING("Missing"),
  ON_ORDER("On order"),
  ORDER_CLOSED("Order closed"),
  IN_PROCESS("In process"),
  CLAIMED_RETURNED("Claimed returned"),
  LOST_AND_PAID("Lost and paid"),
  INTELLECTUAL_ITEM("Intellectual item"),
  IN_PROCESS_NON_REQUESTABLE("In process (non-requestable)"),
  UNAVAILABLE("Unavailable"),
  UNKNOWN("Unknown"),
  RESTRICTED("Restricted"),
  RETRIEVING_FROM_ASR("Retrieving from ASR"),
  MISSING_FROM_ASR("Missing from ASR");

  private final String name;

  @JsonCreator
  public static InventoryItemStatus fromStatus(String name) {
    return Arrays.stream(values())
      .filter(inventoryItemStatus -> inventoryItemStatus.name.equalsIgnoreCase(name))
      .findFirst()
      .orElse(null);
  }

  public boolean isAvailable() {
    return AVAILABLE.equals(this);
  }

  public boolean isInTransit() {
    return IN_TRANSIT.equals(this);
  }

  public boolean isCheckedOut() {
    return CHECKED_OUT.equals(this);
  }
}
