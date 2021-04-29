package org.folio.innreach.domain.service;

import org.folio.innreach.domain.dto.CentralServerDTO;

import java.util.List;
import java.util.UUID;

public interface CentralServerService {
  CentralServerDTO createCentralServer(CentralServerDTO centralServerDTO);

  CentralServerDTO getCentralServer(UUID centralServerId);

  List<CentralServerDTO> getAllCentralServers();

  CentralServerDTO updateCentralServer(UUID centralServerId, CentralServerDTO centralServerDTO);

  void deleteCentralServer(UUID centralServerId);
}
