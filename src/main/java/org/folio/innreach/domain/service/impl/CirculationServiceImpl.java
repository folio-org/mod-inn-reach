package org.folio.innreach.domain.service.impl;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.exception.CirculationProcessException;
import org.folio.innreach.domain.service.CirculationService;
import org.folio.innreach.domain.service.impl.processor.InnReachCirculationProcessor;
import org.folio.innreach.dto.CirculationRequestDTO;
import org.folio.innreach.dto.InnReachResponseDTO;

@Log4j2
@Service
@RequiredArgsConstructor
public class CirculationServiceImpl implements CirculationService {

  private final List<InnReachCirculationProcessor> innReachCirculationProcessors;

  @Override
  public InnReachResponseDTO processCirculationRequest(String trackingId, String centralCode, String circulationOperationName, CirculationRequestDTO circulationRequest) {
    var circulationProcessor = innReachCirculationProcessors.stream()
      .filter(processor -> processor.canProcess(circulationOperationName))
      .findFirst()
      .orElseThrow(() -> new CirculationProcessException("Can't find processor for circulation operation: " + circulationOperationName));

    log.info("Circulation processor for circulation operation [{}] found! Start to process circulation...", circulationOperationName);

    return circulationProcessor.process(trackingId, centralCode, circulationRequest);
  }
}
