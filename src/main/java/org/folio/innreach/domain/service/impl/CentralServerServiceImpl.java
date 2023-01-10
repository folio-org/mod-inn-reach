package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.util.ListUtils.mapItems;

import java.util.ArrayList;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.CentralServerCredentials;
import org.folio.innreach.domain.entity.LocalAgency;
import org.folio.innreach.domain.entity.LocalServerCredentials;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.CentralServersDTO;
import org.folio.innreach.external.service.InnReachAuthExternalService;
import org.folio.innreach.mapper.CentralServerMapper;
import org.folio.innreach.repository.CentralServerRepository;
import org.folio.innreach.repository.LocalAgencyRepository;
import org.folio.spring.data.OffsetRequest;

@RequiredArgsConstructor
@Log4j2
@Service
public class CentralServerServiceImpl implements CentralServerService {

  private final CentralServerRepository centralServerRepository;
  private final CentralServerMapper centralServerMapper;
  private final InnReachAuthExternalService innReachAuthExternalService;
  private final PasswordEncoder passwordEncoder;
  private final LocalAgencyRepository localAgencyRepository;

  @Override
  @Transactional
  public CentralServerDTO createCentralServer(CentralServerDTO centralServerDTO) {
    log.debug("createCentralServer:: parameters centralServerDTO: {}", centralServerDTO);
    checkCentralServerConnection(centralServerDTO);

    var centralServer = centralServerMapper.mapToCentralServer(centralServerDTO);

    centralServer.getLocalAgencies().forEach(la -> la.setCentralServer(centralServer));

    var localServerCredentials = centralServer.getLocalServerCredentials();
    if (localServerCredentials != null) {
      hashAndSaltLocalServerCredentials(localServerCredentials);
    }

    var createdCentralServer = centralServerRepository.save(centralServer);

    Assert.isTrue(localAgencyRepository.findLibraryIdsAssignedToMultipleAgencies(createdCentralServer.getId()).isEmpty(),
      "FOLIO library may only be associated with one agency per central server");

    log.info("createCentralServer:: result: {}", centralServerMapper.mapToCentralServerDTO(createdCentralServer));
    return centralServerMapper.mapToCentralServerDTO(createdCentralServer);
  }

  private void checkCentralServerConnection(CentralServerDTO centralServerDTO) {
    log.debug("checkCentralServerConnection:: parameters centralServerDTO: {}", centralServerDTO);
    log.debug("Get an access token to check the connection to the Central Server with URI: {}",
      centralServerDTO.getCentralServerAddress());

    var centralServerConnectionDetailsDTO = CentralServerConnectionDetailsDTO.builder()
      .id(centralServerDTO.getId())
      .connectionUrl(centralServerDTO.getCentralServerAddress())
      .localCode(centralServerDTO.getLocalServerCode())
      .key(centralServerDTO.getCentralServerKey())
      .secret(centralServerDTO.getCentralServerSecret())
      .build();

    log.info("checkCentralServerConnection:: Access token retrieved");
    innReachAuthExternalService.getAccessToken(centralServerConnectionDetailsDTO);
  }

  private void hashAndSaltLocalServerCredentials(LocalServerCredentials localServerCredentials) {
    log.debug("hashAndSaltLocalServerCredentials:: parameters localServerCredentials: {}", localServerCredentials);
    localServerCredentials.setLocalServerSecret(passwordEncoder.encode(localServerCredentials.getLocalServerSecret()));
  }

  @Override
  @Transactional(readOnly = true)
  public CentralServerDTO getCentralServer(UUID centralServerId) {
    log.debug("getCentralServer:: parameters centralServerId: {}", centralServerId);
    var centralServer = fetchOne(centralServerId);
    log.info("getCentralServer:: result: {}", centralServerMapper.mapToCentralServerDTO(centralServer));
    return centralServerMapper.mapToCentralServerDTO(centralServer);
  }

  @Override
  @Transactional(readOnly = true)
  public CentralServerDTO getCentralServerByCentralCode(String code) {
    log.debug("getCentralServerByCentralCode:: parameters code: {}", code);
    var centralServer = centralServerRepository.fetchOneByCentralCode(code)
      .orElseThrow(() -> new EntityNotFoundException("Central server with code: " + code + " not found"));

    log.info("getCentralServerByCentralCode:: result: {}", centralServerMapper.mapToCentralServerDTO(centralServer));
    return centralServerMapper.mapToCentralServerDTO(centralServer);
  }

  @Override
  public UUID getCentralServerIdByCentralCode(String code) {
    log.debug("getCentralServerIdByCentralCode:: parameters code: {}", code);
    return centralServerRepository.getIdByCentralCode(code)
      .orElseThrow(() -> new EntityNotFoundException("Central server with code: " + code + " not found"));
  }

  private CentralServer fetchOne(UUID centralServerId) {
    log.debug("fetchOne:: parameters centralServerId: {}", centralServerId);
    return centralServerRepository.fetchOne(centralServerId)
      .orElseThrow(() -> new EntityNotFoundException("Central server with ID: " + centralServerId + " not found"));
  }

  @Override
  @Transactional(readOnly = true)
  public CentralServersDTO getAllCentralServers(int offset, int limit) {
    log.debug("getAllCentralServers:: parameters offset: {}, limit: {}", offset, limit);
    Page<UUID> ids = centralServerRepository.getIds(new OffsetRequest(offset, limit));

    var centralServerDTOS = mapItems(centralServerRepository.fetchAllById(ids.getContent()),
      centralServerMapper::mapToCentralServerDTO);

    log.info("getAllCentralServers:: result: {}", centralServerDTOS);
    return new CentralServersDTO()
      .centralServers(centralServerDTOS)
      .totalRecords((int) ids.getTotalElements());
  }

  @Override
  @Transactional
  public CentralServerDTO updateCentralServer(UUID centralServerId, CentralServerDTO centralServerDTO) {
    log.debug("updateCentralServer:: parameters centralServerId: {}, centralServerDTO: {}", centralServerId, centralServerDTO);
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

    Assert.isTrue(localAgencyRepository.findLibraryIdsAssignedToMultipleAgencies(centralServer.getId()).isEmpty(),
      "FOLIO library may only be associated with one agency per central server");

    log.info("updateCentralServer:: result: {}", centralServerMapper.mapToCentralServerDTO(centralServer));
    return centralServerMapper.mapToCentralServerDTO(centralServer);
  }

  private void updateCentralServer(CentralServer centralServer, CentralServer updatedCentralServer) {
    centralServer.setName(updatedCentralServer.getName());
    centralServer.setDescription(updatedCentralServer.getDescription());
    centralServer.setLocalServerCode(updatedCentralServer.getLocalServerCode());
    centralServer.setCentralServerCode(updatedCentralServer.getCentralServerCode());
    centralServer.setCentralServerAddress(updatedCentralServer.getCentralServerAddress());
    centralServer.setLoanTypeId(updatedCentralServer.getLoanTypeId());
    centralServer.setCheckPickupLocation(updatedCentralServer.getCheckPickupLocation());
  }

  private void updateCentralServerCredentials(CentralServer centralServer, CentralServerCredentials updatedCentralServerCredentials) {
    var centralServerCredentials = centralServer.getCentralServerCredentials();
    centralServerCredentials.setCentralServerKey(updatedCentralServerCredentials.getCentralServerKey());
    centralServerCredentials.setCentralServerSecret(updatedCentralServerCredentials.getCentralServerSecret());
  }

  private void updateLocalServerCredentials(CentralServer centralServer, LocalServerCredentials updatedLocalServerCredentials) {
    log.debug("updateLocalServerCredentials:: parameters centralServer: {}, updatedLocalServerCredentials: {}", centralServer, updatedLocalServerCredentials);
    var localServerCredentials = centralServer.getLocalServerCredentials();

    if (localServerCredentials == null) {
      log.debug("Local server credentials didn't exist. Save new and perform hashing, salting.");
      hashAndSaltLocalServerCredentials(updatedLocalServerCredentials);
      centralServer.setLocalServerCredentials(updatedLocalServerCredentials);

    } else if (existingCredentialsShouldBeUpdated(localServerCredentials, updatedLocalServerCredentials)) {
      log.debug("Local server credentials existed. Update and perform re-hashing and re-salting.");
      hashAndSaltLocalServerCredentials(updatedLocalServerCredentials);

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
    log.debug("updateLocalAgencies:: parameters centralServer: {}, updatedCentralServer: {}", centralServer, updatedCentralServer);
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
    log.info("updateLocalAgencies:: Local agency updated");
  }

  @Override
  @Transactional
  public void deleteCentralServer(UUID centralServerId) {
    log.debug("deleteCentralServer:: parameters centralServerId: {}", centralServerId);
    var centralServer = centralServerRepository.findById(centralServerId)
      .orElseThrow(() -> new EntityNotFoundException("Central server with ID: " + centralServerId + " not found"));

    centralServerRepository.delete(centralServer);
    log.info("deleteCentralServer:: Central server deleted");
  }

  @Override
  public CentralServerConnectionDetailsDTO getCentralServerConnectionDetails(UUID centralServerId) {
    log.debug("getCentralServerConnectionDetails:: parameters centralServerId: {}", centralServerId);
    return centralServerRepository.fetchConnectionDetails(centralServerId)
      .orElseThrow(() -> new EntityNotFoundException("Central server with ID: " + centralServerId + " not found"));
  }

  @Override
  public CentralServerConnectionDetailsDTO getConnectionDetailsByCode(String centralCode) {
    log.debug("getConnectionDetailsByCode:: parameters centralCode: {}", centralCode);
    return centralServerRepository.fetchConnectionDetailsByCentralCode(centralCode)
      .orElseThrow(() -> new EntityNotFoundException("Central server with code: " + centralCode + " not found"));
  }
}
