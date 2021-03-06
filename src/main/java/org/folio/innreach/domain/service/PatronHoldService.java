package org.folio.innreach.domain.service;

import org.springframework.scheduling.annotation.Async;

import org.folio.innreach.domain.entity.InnReachTransaction;

public interface PatronHoldService {

  @Async
  void createVirtualItems(InnReachTransaction transaction);

  @Async
  void updateVirtualItems(InnReachTransaction transaction);

  void addItemBarcode(InnReachTransaction transaction, String itemBarcode);

  void addItemBarcodeAndCallNumber(InnReachTransaction transaction, String itemBarcode, String callNumber);
}
