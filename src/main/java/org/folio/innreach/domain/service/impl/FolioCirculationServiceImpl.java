package org.folio.innreach.domain.service.impl;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO;
import org.folio.innreach.domain.service.FolioCirculationService;

@Service
@RequiredArgsConstructor
public class FolioCirculationServiceImpl implements FolioCirculationService {

  private final CirculationClient circulationClient;

  @Override
  public RequestDTO moveRequest(UUID requestId, CirculationClient.MoveRequest payload) {
    return circulationClient.moveRequest(requestId, payload);
  }
}
