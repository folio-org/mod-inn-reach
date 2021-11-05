package org.folio.innreach.domain.processor;

import org.folio.innreach.dto.CirculationRequestDTO;
import org.folio.innreach.dto.InnReachResponseDTO;

public interface InnReachCirculationProcessor {

  boolean canProcess(String circulationOperationName);

  InnReachResponseDTO process(String trackingId, String centralCode, CirculationRequestDTO request);

}
