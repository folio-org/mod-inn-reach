package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.InnReachLocationDTO;
import org.folio.innreach.dto.InnReachLocationsDTO;

public interface InnReachLocationService {

  InnReachLocationDTO createInnReachLocation(InnReachLocationDTO innReachLocationDTO);

  InnReachLocationDTO getInnReachLocation(UUID innReachLocationId);

  InnReachLocationsDTO getInnReachLocations(Iterable<UUID> innReachLocationIds);

  InnReachLocationsDTO getAllInnReachLocations(Integer offset, Integer limit);

  InnReachLocationDTO updateInnReachLocation(UUID innReachLocationId, InnReachLocationDTO innReachLocationDTO);

  void deleteInnReachLocation(UUID innReachLocationId);
}
