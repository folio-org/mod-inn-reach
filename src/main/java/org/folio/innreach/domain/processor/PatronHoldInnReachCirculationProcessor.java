package org.folio.innreach.domain.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.CirculationOperation;
import org.folio.innreach.domain.InnReachResponseStatus;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.mapper.InnReachTransactionHoldMapper;
import org.folio.innreach.mapper.InnReachTransactionPickupLocationMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Log4j2
@Component
@RequiredArgsConstructor
public class PatronHoldInnReachCirculationProcessor implements InnReachCirculationProcessor {

  private final InnReachTransactionRepository transactionRepository;

  private final InnReachTransactionHoldMapper transactionHoldMapper;
  private final InnReachTransactionPickupLocationMapper pickupLocationMapper;

  @Override
  public boolean canProcess(String circulationOperationName) {
    return CirculationOperation.PATRON_HOLD.getOperationName().equals(circulationOperationName);
  }

  @Override
  @Transactional
  public InnReachResponseDTO process(String trackingId, String centralCode, TransactionHoldDTO transactionHold) {
    var innReachTransaction = transactionRepository.findByTrackingIdAndAndCentralServerCode(trackingId, centralCode);

    if (innReachTransaction.isPresent()) {
      log.info("Transaction patron hold with trackingId [{}] and centralCode [{}] exists, start to update...", trackingId, centralCode);
      updateTransactionPatronHold((TransactionPatronHold) innReachTransaction.get().getHold(), transactionHold);
    } else {
      log.info("Transaction patron hold with trackingId [{}] and centralCode [{}] doesn't exist, create a new one...", trackingId, centralCode);
      InnReachTransaction newTransactionPatronHold = createTransactionWithPatronHold(trackingId, centralCode, transactionHold);
      transactionRepository.save(newTransactionPatronHold);
    }

    return new InnReachResponseDTO().status(InnReachResponseStatus.OK.getResponseStatus());
  }

  private void updateTransactionPatronHold(TransactionPatronHold existingTransactionPatronHold, TransactionHoldDTO transactionHold) {
    // update transaction patron hold
    BeanUtils.copyProperties(transactionHold, existingTransactionPatronHold,
      "pickupLocation", "id", "createdBy", "updatedBy", "createdDate", "updatedDate");

    // update pickupLocation
    var pickupLocation = pickupLocationMapper.fromString(transactionHold.getPickupLocation());
    BeanUtils.copyProperties(pickupLocation, existingTransactionPatronHold.getPickupLocation(),
      "id", "createdBy", "updatedBy", "createdDate", "updatedDate");
  }

  private InnReachTransaction createTransactionWithPatronHold(String trackingId, String centralCode, TransactionHoldDTO transactionHold) {
    var newInnReachTransaction = new InnReachTransaction();
    newInnReachTransaction.setHold(transactionHoldMapper.toPatronHold(transactionHold));
    newInnReachTransaction.setCentralServerCode(centralCode);
    newInnReachTransaction.setTrackingId(trackingId);
    newInnReachTransaction.setState(InnReachTransaction.TransactionState.PATRON_HOLD);
    newInnReachTransaction.setType(InnReachTransaction.TransactionType.PATRON);
    return newInnReachTransaction;
  }

}
