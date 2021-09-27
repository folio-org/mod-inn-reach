package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.CentralServersDTO;

public interface CentralServerService {
  CentralServerDTO createCentralServer(CentralServerDTO centralServerDTO);

  CentralServerDTO getCentralServer(UUID centralServerId);

  CentralServerDTO getCentralServerByCentralCode(String code);

  CentralServersDTO getAllCentralServers(int offset, int limit);

  CentralServerDTO updateCentralServer(UUID centralServerId, CentralServerDTO centralServerDTO);

  void deleteCentralServer(UUID centralServerId);

  CentralServerConnectionDetailsDTO getCentralServerConnectionDetails(UUID centralServerId);
}
