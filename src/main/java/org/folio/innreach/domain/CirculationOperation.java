package org.folio.innreach.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum CirculationOperation {

  PATRON_HOLD("patronhold"),
  CANCEL_ITEM_HOLD ("cancel_itemhold"),
  ITEM_SHIPPED("itemshipped");

  private final String operationName;

}
