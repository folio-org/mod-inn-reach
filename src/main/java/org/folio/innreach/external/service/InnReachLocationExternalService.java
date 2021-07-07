package org.folio.innreach.external.service;

import java.util.List;

import org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO;
import org.folio.innreach.external.dto.InnReachLocationDTO;

public interface InnReachLocationExternalService {

  void updateAllLocations(CentralServerConnectionDetailsDTO connectionDetails, List<InnReachLocationDTO> updatedLocations);
}
