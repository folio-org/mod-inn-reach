package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.ItemHoldCheckOutResponseDTO;
import org.folio.innreach.dto.PatronHoldCheckInResponseDTO;

public interface InnReachTransactionActionService {

  PatronHoldCheckInResponseDTO checkInPatronHoldItem(UUID transactionId, UUID servicePointId);

  ItemHoldCheckOutResponseDTO checkOutItemHoldItem(String itemBarcode, UUID servicePointId);

}
