package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;

import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import org.folio.innreach.domain.InnReachResponseStatus;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.exception.CirculationProcessException;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.CirculationService;
import org.folio.innreach.domain.service.PatronHoldService;
import org.folio.innreach.domain.service.impl.processor.InnReachCirculationProcessor;
import org.folio.innreach.dto.CirculationRequestDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.PatronHoldDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.dto.TransferRequestDTO;
import org.folio.innreach.mapper.InnReachTransactionHoldMapper;
import org.folio.innreach.mapper.InnReachTransactionPickupLocationMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class CirculationServiceImpl implements CirculationService {

  private final List<InnReachCirculationProcessor> innReachCirculationProcessors;
  private final InnReachTransactionRepository transactionRepository;
  private final InnReachTransactionHoldMapper transactionHoldMapper;
  private final InnReachTransactionPickupLocationMapper pickupLocationMapper;
  private final PatronHoldService patronHoldService;


  @Override
  public InnReachResponseDTO processCirculationRequest(String trackingId, String centralCode, String circulationOperationName, CirculationRequestDTO circulationRequest) {
    var circulationProcessor = innReachCirculationProcessors.stream()
      .filter(processor -> processor.canProcess(circulationOperationName))
      .findFirst()
      .orElseThrow(() -> new CirculationProcessException("Can't find processor for circulation operation: " + circulationOperationName));

    log.info("Circulation processor for circulation operation [{}] found! Start to process circulation...", circulationOperationName);

    return circulationProcessor.process(trackingId, centralCode, circulationRequest);
  }

  @Override
  public InnReachResponseDTO initiatePatronHold(String trackingId, String centralCode, PatronHoldDTO patronHold) {
    var innReachTransaction = transactionRepository.findByTrackingIdAndCentralServerCode(trackingId, centralCode);
    var transactionHold = transactionHoldMapper.mapRequest(patronHold);

    if (innReachTransaction.isPresent()) {
      log.info("Transaction patron hold with trackingId [{}] and centralCode [{}] exists, start to update...",
          trackingId, centralCode);

      updateTransactionPatronHold((TransactionPatronHold) innReachTransaction.get().getHold(), transactionHold);

      patronHoldService.updateVirtualItems(innReachTransaction.get());
    } else {
      log.info("Transaction patron hold with trackingId [{}] and centralCode [{}] doesn't exist, create a new one...",
          trackingId, centralCode);

      InnReachTransaction newTransactionWithPatronHold = createTransactionWithPatronHold(trackingId, centralCode,
          transactionHold);
      var transaction = transactionRepository.save(newTransactionWithPatronHold);

      patronHoldService.createVirtualItems(transaction);
    }

    return new InnReachResponseDTO().status(InnReachResponseStatus.OK.getResponseStatus());
  }

  @Override
  public InnReachResponseDTO transferItem(String trackingId, String centralCode, TransferRequestDTO request) {
    var transaction = getTransaction(trackingId, centralCode);

    validateItemIdsEqual(request, transaction);

    transaction.getHold().setItemId(request.getNewItemId());
    transaction.setState(TRANSFER);

    return new InnReachResponseDTO().status("ok").reason("success");
  }

  private void updateTransactionPatronHold(TransactionPatronHold existingTransactionPatronHold,
      TransactionHoldDTO transactionHold) {
    // update transaction patron hold
    BeanUtils.copyProperties(transactionHold, existingTransactionPatronHold,
        "pickupLocation", "id", "createdBy", "updatedBy", "createdDate", "updatedDate",
        "folioPatronId", "folioInstanceId", "folioHoldingId", "folioItemId",
        "folioRequestId", "folioLoanId", "folioPatronBarcode", "folioItemBarcode");

    // update pickupLocation
    var pickupLocation = pickupLocationMapper.fromString(transactionHold.getPickupLocation());
    BeanUtils.copyProperties(pickupLocation, existingTransactionPatronHold.getPickupLocation(),
        "id", "createdBy", "updatedBy", "createdDate", "updatedDate");
  }

  private InnReachTransaction createTransactionWithPatronHold(String trackingId, String centralCode,
      TransactionHoldDTO transactionHold) {
    var newInnReachTransaction = new InnReachTransaction();
    newInnReachTransaction.setHold(transactionHoldMapper.toPatronHold(transactionHold));
    newInnReachTransaction.setCentralServerCode(centralCode);
    newInnReachTransaction.setTrackingId(trackingId);
    newInnReachTransaction.setState(InnReachTransaction.TransactionState.PATRON_HOLD);
    newInnReachTransaction.setType(InnReachTransaction.TransactionType.PATRON);
    return newInnReachTransaction;
  }

  private void validateItemIdsEqual(TransferRequestDTO request, InnReachTransaction transaction) {
    var trxItemId = transaction.getHold().getItemId();
    var reqItemId = request.getItemId();

    Assert.isTrue(Objects.equals(reqItemId, trxItemId),
        String.format("Item id [%s] from the request doesn't match with item id [%s] in the stored transaction",
            reqItemId, trxItemId));
  }

  private InnReachTransaction getTransaction(String trackingId, String centralCode) {
    return transactionRepository.findByTrackingIdAndCentralServerCode(trackingId, centralCode)
        .orElseThrow(() -> new EntityNotFoundException(String.format(
            "InnReach transaction with tracking id [%s] and central code [%s] not found", trackingId, centralCode)));
  }

}
