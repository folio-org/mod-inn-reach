package org.folio.innreach.domain.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.dto.CentralServerDTO;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.CentralServerCredentials;
import org.folio.innreach.domain.entity.LocalAgency;
import org.folio.innreach.domain.entity.LocalServerCredentials;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.external.dto.AccessTokenRequestDTO;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.mapper.CentralServerMapper;
import org.folio.innreach.repository.CentralServerRepository;

@RequiredArgsConstructor
@Log4j2
@Service
public class CentralServerServiceImpl implements CentralServerService {

  private final CentralServerRepository centralServerRepository;
  private final CentralServerMapper centralServerMapper;
  private final InnReachExternalService innReachExternalService;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public CentralServerDTO createCentralServer(CentralServerDTO centralServerDTO) {
    checkCentralServerConnection(centralServerDTO);

    var centralServer = centralServerMapper.mapToCentralServer(centralServerDTO);

    centralServer.getLocalAgencies().forEach(la -> la.setCentralServer(centralServer));

    var localServerCredentials = centralServer.getLocalServerCredentials();
    if (localServerCredentials != null) {
      hashAndSaltLocalServerCredentials(localServerCredentials);
    }

    var createdCentralServer = centralServerRepository.save(centralServer);

    return centralServerMapper.mapToCentralServerDTO(createdCentralServer);
  }

  private void checkCentralServerConnection(CentralServerDTO centralServerDTO) {
    log.debug("Get an access token to check the connection to the Central Server with URI: {}",
      centralServerDTO.getCentralServerAddress());

    innReachExternalService.getAccessToken(new AccessTokenRequestDTO(
      centralServerDTO.getCentralServerAddress(),
      centralServerDTO.getCentralServerKey(),
      centralServerDTO.getCentralServerSecret())
    );
  }

  private void hashAndSaltLocalServerCredentials(LocalServerCredentials localServerCredentials) {
    localServerCredentials.setLocalServerSecret(passwordEncoder.encode(localServerCredentials.getLocalServerSecret()));
    localServerCredentials.setLocalServerSecretSalt(passwordEncoder.encode(UUID.randomUUID().toString()));
  }

  @Override
  @Transactional(readOnly = true)
  public CentralServerDTO getCentralServer(UUID centralServerId) {
    var centralServer = fetchOne(centralServerId);
    return centralServerMapper.mapToCentralServerDTO(centralServer);
  }

  private CentralServer fetchOne(UUID centralServerId) {
    return centralServerRepository.fetchOne(centralServerId)
      .orElseThrow(() -> new EntityNotFoundException("Central server with ID: " + centralServerId + " not found"));
  }

  @Override
  @Transactional(readOnly = true)
  public List<CentralServerDTO> getAllCentralServers() {
    return centralServerRepository.fetchAll()
      .stream()
      .map(centralServerMapper::mapToCentralServerDTO)
      .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public CentralServerDTO updateCentralServer(UUID centralServerId, CentralServerDTO centralServerDTO) {
    var centralServer = fetchOne(centralServerId);
    var updatedCentralServer = centralServerMapper.mapToCentralServer(centralServerDTO);

    updateCentralServer(centralServer, updatedCentralServer);

    updateCentralServerCredentials(centralServer, updatedCentralServer.getCentralServerCredentials());

    var updatedLocalServerCredentials = updatedCentralServer.getLocalServerCredentials();
    if (updatedLocalServerCredentials != null) {
      updateLocalServerCredentials(centralServer, updatedLocalServerCredentials);
    }

    updateLocalAgencies(centralServer, updatedCentralServer);

    centralServerRepository.save(centralServer);

    return centralServerMapper.mapToCentralServerDTO(centralServer);
  }

  private void updateCentralServer(CentralServer centralServer, CentralServer updatedCentralServer) {
    centralServer.setName(updatedCentralServer.getName());
    centralServer.setDescription(updatedCentralServer.getDescription());
    centralServer.setLocalServerCode(updatedCentralServer.getLocalServerCode());
    centralServer.setCentralServerAddress(updatedCentralServer.getCentralServerAddress());
    centralServer.setLoanTypeId(updatedCentralServer.getLoanTypeId());
  }

  private void updateCentralServerCredentials(CentralServer centralServer, CentralServerCredentials updatedCentralServerCredentials) {
    var centralServerCredentials = centralServer.getCentralServerCredentials();
    centralServerCredentials.setCentralServerKey(updatedCentralServerCredentials.getCentralServerKey());
    centralServerCredentials.setCentralServerSecret(updatedCentralServerCredentials.getCentralServerSecret());
  }

  private void updateLocalServerCredentials(CentralServer centralServer, LocalServerCredentials updatedLocalServerCredentials) {
    var localServerCredentials = centralServer.getLocalServerCredentials();

    if (localServerCredentials == null) {
      log.debug("Local server credentials didn't exist. Save new and perform hashing, salting.");
      hashAndSaltLocalServerCredentials(updatedLocalServerCredentials);
      centralServer.setLocalServerCredentials(updatedLocalServerCredentials);

    } else if (existingCredentialsShouldBeUpdated(localServerCredentials, updatedLocalServerCredentials)) {
      log.debug("Local server credentials existed. Update and perform re-hashing and re-salting.");
      hashAndSaltLocalServerCredentials(updatedLocalServerCredentials);

      localServerCredentials.setLocalServerSecretSalt(updatedLocalServerCredentials.getLocalServerSecretSalt());
      localServerCredentials.setLocalServerKey(updatedLocalServerCredentials.getLocalServerKey());
      localServerCredentials.setLocalServerSecret(updatedLocalServerCredentials.getLocalServerSecret());
    }
  }

  private boolean existingCredentialsShouldBeUpdated(LocalServerCredentials localServerCredentials,
      LocalServerCredentials updatedLocalServerCredentials) {
    // LocalServerCredentials need to be re-hashed and re-salted only if they have been updated
    return !localServerCredentials.getLocalServerSecret().equals(updatedLocalServerCredentials.getLocalServerSecret());
  }

  private void updateLocalAgencies(CentralServer centralServer, CentralServer updatedCentralServer) {
    var currentLocalAgencies = new ArrayList<>(centralServer.getLocalAgencies());
    var updatedLocalAgencies = updatedCentralServer.getLocalAgencies();

    // 1. Remove the existing database records that are no longer found in the incoming collection.
    var localAgenciesToDelete = new ArrayList<>(currentLocalAgencies);
    localAgenciesToDelete.removeAll(updatedLocalAgencies);
    localAgenciesToDelete.forEach(centralServer::removeLocalAgency);

    // 2. Add the records found in the incoming collection, which cannot be found in the current database snapshot.
    var newLocalAgencies = new ArrayList<>(updatedLocalAgencies);
    newLocalAgencies.removeAll(currentLocalAgencies);
    newLocalAgencies.forEach(centralServer::addLocalAgency);

    // 3. Update the existing database records which can be found in the incoming collection.
    updatedLocalAgencies.removeAll(newLocalAgencies);

    for (LocalAgency updLocalAgency : updatedLocalAgencies) {
      updLocalAgency.setCentralServer(centralServer);
      centralServer.getLocalAgencies().set(centralServer.getLocalAgencies().indexOf(updLocalAgency), updLocalAgency);
    }
  }

  @Override
  @Transactional
  public void deleteCentralServer(UUID centralServerId) {
    var centralServer = centralServerRepository.findById(centralServerId)
      .orElseThrow(() -> new EntityNotFoundException("Central server with ID: " + centralServerId + " not found"));

    centralServerRepository.delete(centralServer);
  }
}
