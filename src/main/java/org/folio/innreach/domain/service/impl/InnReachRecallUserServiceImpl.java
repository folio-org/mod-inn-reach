package org.folio.innreach.domain.service.impl;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.entity.InnReachRecallUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.InnReachRecallUserService;
import org.folio.innreach.dto.InnReachRecallUserDTO;
import org.folio.innreach.mapper.InnReachRecallUserMapper;
import org.folio.innreach.repository.CentralServerRepository;
import org.folio.innreach.repository.InnReachRecallUserRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class InnReachRecallUserServiceImpl implements InnReachRecallUserService {

  private final CentralServerRepository centralServerRepository;
  private final InnReachRecallUserRepository recallUserRepository;
  private final InnReachRecallUserMapper recallUserMapper;

  @Override
  @Transactional
  public InnReachRecallUserDTO saveInnReachRecallUser(UUID centralServerId, InnReachRecallUserDTO innReachRecallUserDTO) {
    log.debug("saveInnReachRecallUser:: parameters centralServerId: {}, innReachRecallUserDTO: {}", centralServerId, innReachRecallUserDTO);
    CentralServer centralServer = centralServerRepository
      .findById(centralServerId)
      .orElseThrow(() -> new EntityNotFoundException("Central server with id " + centralServerId + " not found"));

    var recallUser = recallUserMapper.toEntity(innReachRecallUserDTO);
    var savedRecallUser = recallUserRepository.save(recallUser);

    centralServer.setInnReachRecallUser(savedRecallUser);

    log.info("saveInnReachRecallUser:: result: {}", recallUserMapper.toDto(savedRecallUser));
    return recallUserMapper.toDto(savedRecallUser);
  }

  @Override
  public InnReachRecallUserDTO getInnReachRecallUser(UUID centralServerId) {
    log.debug("getInnReachRecallUser:: parameters centralServerId: {}", centralServerId);
    var centralServer = fetchCentralServerWithRecallUser(centralServerId);
    log.info("getInnReachRecallUser:: result: {}", recallUserMapper.toDto(centralServer.getInnReachRecallUser()));
    return recallUserMapper.toDto(centralServer.getInnReachRecallUser());
  }

  @Override
  @Transactional
  public InnReachRecallUserDTO updateInnReachRecallUser(UUID centralServerId, InnReachRecallUserDTO innReachRecallUserDTO) {
    log.debug("updateInnReachRecallUser:: parameters centralServerId: {}, innReachRecallUserDTO: {}", centralServerId, innReachRecallUserDTO);
    var centralServer = fetchCentralServerWithRecallUser(centralServerId);

    var innReachRecallUser = centralServer.getInnReachRecallUser();
    innReachRecallUser.setUserId(innReachRecallUserDTO.getUserId());

    log.info("updateInnReachRecallUser:: result: {}", recallUserMapper.toDto(centralServer.getInnReachRecallUser()));
    return recallUserMapper.toDto(centralServer.getInnReachRecallUser());
  }

  @Override
  public InnReachRecallUser getRecallUserForCentralServer(String centralCode) {
    log.debug("getRecallUserForCentralServer:: parameters centralCode: {}", centralCode);
    return centralServerRepository.fetchOneByCentralCode(centralCode)
      .map(CentralServer::getInnReachRecallUser)
      .orElseThrow(() -> new EntityNotFoundException("Recall user is not set for central server with code = " + centralCode));
  }

  private CentralServer fetchCentralServerWithRecallUser(UUID centralServerId) {
    log.debug("fetchCentralServerWithRecallUser:: parameters centralServerId: {}", centralServerId);
    return centralServerRepository
      .fetchOneWithRecallUser(centralServerId)
      .orElseThrow(() -> new EntityNotFoundException("Central server with id " + centralServerId + " not found"));
  }

}
