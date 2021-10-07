package org.folio.innreach.domain.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.CirculationOperation;
import org.folio.innreach.domain.InnReachResponseStatus;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.entity.TransactionPickupLocation;
import org.folio.innreach.dto.CirculationRequestDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Log4j2
@Component
@RequiredArgsConstructor
public class PatronHoldInnReachCirculationProcessor implements InnReachCirculationProcessor {

  private static final String PICKUP_LOCATION_STRING_DELIMITER = ":";

  private static final int PICKUP_LOCATION_CODE_POSITION = 0;
  private static final int DISPLAY_NAME_POSITION = 1;
  private static final int PRINT_NAME_POSITION = 2;
  private static final int DELIVERY_STOP_POSITION = 3;

  private final InnReachTransactionRepository transactionRepository;

  @Override
  public boolean canProcess(String circulationOperationName) {
    return CirculationOperation.PATRON_HOLD.getOperationName().equals(circulationOperationName);
  }

  @Override
  @Transactional
  public InnReachResponseDTO process(String trackingId, String centralCode, CirculationRequestDTO circulationRequest) {
    var innReachTransaction = transactionRepository.findByTrackingIdAndAndCentralServerCode(trackingId, centralCode);

    if (innReachTransaction.isPresent()) {
      log.info("Transaction patron hold with trackingId [{}] and centralCode [{}] exists, start to update...", trackingId, centralCode);
      updateTransactionPatronHold((TransactionPatronHold) innReachTransaction.get().getHold(), circulationRequest);
    } else {
      log.info("Transaction patron hold with trackingId [{}] and centralCode [{}] doesn't exist, create a new one...", trackingId, centralCode);
      InnReachTransaction newTransactionPatronHold = createTransactionPatronHold(trackingId, centralCode, circulationRequest);
      transactionRepository.save(newTransactionPatronHold);
    }

    var innReachResponseDTO = new InnReachResponseDTO();
    innReachResponseDTO.setStatus(InnReachResponseStatus.OK.getResponseStatus());
    return innReachResponseDTO;
  }

  private void updateTransactionPatronHold(TransactionPatronHold existingTransactionPatronHold, CirculationRequestDTO circulationRequest) {
    copyTransactionPatronHoldProperties(existingTransactionPatronHold, circulationRequest);
    var pickupLocation = createPickupLocation(circulationRequest);
    existingTransactionPatronHold.getPickupLocation().setDeliveryStop(pickupLocation.getDeliveryStop());
    existingTransactionPatronHold.getPickupLocation().setPrintName(pickupLocation.getPrintName());
    existingTransactionPatronHold.getPickupLocation().setDisplayName(pickupLocation.getDisplayName());
    existingTransactionPatronHold.getPickupLocation().setDeliveryStop(pickupLocation.getDeliveryStop());
  }

  private void copyTransactionPatronHoldProperties(TransactionPatronHold transactionPatronHold, CirculationRequestDTO circulationRequest) {
    transactionPatronHold.setTransactionTime(circulationRequest.getTransactionTime());
    transactionPatronHold.setPatronId(circulationRequest.getPatronId());
    transactionPatronHold.setPatronAgencyCode(circulationRequest.getPatronAgencyCode());
    transactionPatronHold.setItemAgencyCode(circulationRequest.getItemAgencyCode());
    transactionPatronHold.setItemId(circulationRequest.getItemId());
    transactionPatronHold.setCentralItemType(circulationRequest.getCentralItemType());
    transactionPatronHold.setTitle(circulationRequest.getTitle());
    transactionPatronHold.setAuthor(circulationRequest.getAuthor());
    transactionPatronHold.setCallNumber(circulationRequest.getCallNumber());
    transactionPatronHold.setNeedBefore(circulationRequest.getNeedBefore());
  }

  private InnReachTransaction createTransactionPatronHold(String trackingId, String centralCode, CirculationRequestDTO circulationRequest) {
    var newInnReachTransaction = new InnReachTransaction();
    newInnReachTransaction.setHold(createTransactionPatronHold(circulationRequest));
    newInnReachTransaction.setCentralServerCode(centralCode);
    newInnReachTransaction.setTrackingId(trackingId);
    newInnReachTransaction.setState(InnReachTransaction.TransactionState.PATRON_HOLD);
    newInnReachTransaction.setType(InnReachTransaction.TransactionType.PATRON);
    return newInnReachTransaction;
  }

  private TransactionPatronHold createTransactionPatronHold(CirculationRequestDTO circulationRequest) {
    var transactionPatronHold = new TransactionPatronHold();
    copyTransactionPatronHoldProperties(transactionPatronHold, circulationRequest);
    transactionPatronHold.setPickupLocation(createPickupLocation(circulationRequest));
    return transactionPatronHold;
  }

  private TransactionPickupLocation createPickupLocation(CirculationRequestDTO circulationRequest) {
    var transactionPickupLocation = new TransactionPickupLocation();

    var parsedPickupLocationStr = circulationRequest.getPickupLocation().split(PICKUP_LOCATION_STRING_DELIMITER);

    transactionPickupLocation.setPickupLocCode(parsedPickupLocationStr[PICKUP_LOCATION_CODE_POSITION]);
    transactionPickupLocation.setDisplayName(parsedPickupLocationStr[DISPLAY_NAME_POSITION]);
    transactionPickupLocation.setPrintName(parsedPickupLocationStr[PRINT_NAME_POSITION]);

    if (parsedPickupLocationStr.length == 4) {
      transactionPickupLocation.setDeliveryStop(parsedPickupLocationStr[DELIVERY_STOP_POSITION]);
    }

    return transactionPickupLocation;
  }
}
