package org.folio.innreach.domain.service;

import java.util.List;
import java.util.UUID;

import org.folio.innreach.domain.dto.CentralServerDTO;

public interface CentralServerService {
  CentralServerDTO createCentralServer(CentralServerDTO centralServerDTO);

  CentralServerDTO getCentralServer(UUID centralServerId);

  List<CentralServerDTO> getAllCentralServers();

  CentralServerDTO updateCentralServer(UUID centralServerId, CentralServerDTO centralServerDTO);

  void deleteCentralServer(UUID centralServerId);
}
