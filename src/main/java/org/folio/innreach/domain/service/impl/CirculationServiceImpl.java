package org.folio.innreach.domain.service.impl;

import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.exception.CirculationProcessException;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.CirculationService;
import org.folio.innreach.domain.service.impl.processor.InnReachCirculationProcessor;
import org.folio.innreach.dto.CirculationRequestDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.TransferRequestDTO;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class CirculationServiceImpl implements CirculationService {

  private final List<InnReachCirculationProcessor> innReachCirculationProcessors;
  private final InnReachTransactionRepository transactionRepository;

  @Override
  public InnReachResponseDTO processCirculationRequest(String trackingId, String centralCode, String circulationOperationName, CirculationRequestDTO request) {
    var circulationProcessor = innReachCirculationProcessors.stream()
      .filter(processor -> processor.canProcess(circulationOperationName))
      .findFirst()
      .orElseThrow(() -> new CirculationProcessException("Can't find processor for circulation operation: " + circulationOperationName));

    log.info("Circulation processor for circulation operation [{}] found! Start to process circulation...", circulationOperationName);

    return circulationProcessor.process(trackingId, centralCode, request);
  }

  @Override
  @Transactional
  public InnReachResponseDTO transferItem(String trackingId, String centralCode, TransferRequestDTO request) {
    var transaction = getTransaction(trackingId, centralCode);

    validateItemIdsEqual(request, transaction);

    transaction.getHold().setItemId(request.getNewItemId());

    return new InnReachResponseDTO().status("ok").reason("success");
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
