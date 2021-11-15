package org.folio.innreach.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum CirculationOperation {

  CANCEL_ITEM_HOLD ("cancel_itemhold"),
  CANCEL_PATRON_HOLD("cancelrequest");

  private final String operationName;

}
