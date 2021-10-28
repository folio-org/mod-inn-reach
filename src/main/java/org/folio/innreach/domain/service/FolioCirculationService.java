package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO;

public interface FolioCirculationService {

  RequestDTO moveRequest(UUID requestId, CirculationClient.MoveRequest payload);

}
