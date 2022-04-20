package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.domain.entity.InnReachRecallUser;
import org.folio.innreach.dto.InnReachRecallUserDTO;

public interface InnReachRecallUserService {

  InnReachRecallUserDTO saveInnReachRecallUser(UUID centralServerId, InnReachRecallUserDTO innReachRecallUserDTO);

  InnReachRecallUserDTO getInnReachRecallUser(UUID centralServerId);

  InnReachRecallUserDTO updateInnReachRecallUser(UUID centralServerId, InnReachRecallUserDTO innReachRecallUserDTO);

  InnReachRecallUser getRecallUserForCentralServer(String centralCode);
}
