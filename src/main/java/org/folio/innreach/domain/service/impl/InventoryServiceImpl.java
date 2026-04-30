package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.util.ListUtils.getFirstItem;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.util.CqlQuery;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.HridSettingsClient;
import org.folio.innreach.client.InstanceContributorTypeClient;
import org.folio.innreach.client.InstanceTypeClient;
import org.folio.innreach.client.ServicePointsClient;
import org.folio.innreach.client.ServicePointsUsersClient;
import org.folio.innreach.domain.dto.folio.inventorystorage.ServicePointUserDTO;
import org.folio.innreach.domain.service.InventoryService;

@Log4j2
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

  private final ServicePointsUsersClient servicePointsUsersClient;
  private final ServicePointsClient servicePointsClient;
  private final HridSettingsClient hridSettingsClient;
  private final InstanceTypeClient instanceTypeClient;
  private final InstanceContributorTypeClient nameTypeClient;

  @Override
  public Optional<UUID> findDefaultServicePointIdForUser(UUID userId) {
    log.debug("findDefaultServicePointIdForUser:: parameters userId: {}", userId);
    var cqlQuery = CqlQuery.exactMatch("userId", userId.toString());
    return getFirstItem(servicePointsUsersClient.findServicePointsUsersByQuery(cqlQuery.getQuery()))
      .map(ServicePointUserDTO::getDefaultServicePointId);
  }

  @Override
  public Optional<UUID> findServicePointIdByCode(String code) {
    log.debug("findServicePointIdByCode:: parameters code: {}", code);
    var cqlQuery = CqlQuery.exactMatchByCode(code);
    return getFirstItem(servicePointsClient.findByQuery(cqlQuery.getQuery()))
      .map(ServicePointsClient.ServicePoint::getId);
  }

  @Override
  public InstanceTypeClient.InstanceType queryInstanceTypeByName(String name) {
    log.debug("queryInstanceTypeByName:: parameters name: {}", name);
    var cqlQuery = CqlQuery.exactMatchByName(name);
    return getFirstItem(instanceTypeClient.findByQuery(cqlQuery.getQuery()))
      .orElseThrow(() -> new IllegalArgumentException("Instance type is not found by name: " + name));
  }

  @Override
  public InstanceContributorTypeClient.NameType queryContributorTypeByName(String name) {
    log.debug("queryContributorTypeByName:: parameters name: {}", name);
    var cql = CqlQuery.exactMatchByName(name);
    return getFirstItem(nameTypeClient.findByQuery(cql.getQuery()))
      .orElseThrow(() -> new IllegalArgumentException("Contributor name type is not found by name: " + name));
  }

  @Override
  public HridSettingsClient.HridSettings getHridSettings() {
    return hridSettingsClient.getHridSettings();
  }

}
