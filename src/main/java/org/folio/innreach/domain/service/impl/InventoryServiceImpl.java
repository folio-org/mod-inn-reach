package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.util.ListUtils.getFirstItem;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.HridSettingsClient;
import org.folio.innreach.client.InstanceContributorTypeClient;
import org.folio.innreach.client.InstanceTypeClient;
import org.folio.innreach.client.ServicePointsClient;
import org.folio.innreach.client.ServicePointsUsersClient;
import org.folio.innreach.domain.dto.folio.inventorystorage.ServicePointUserDTO;
import org.folio.innreach.domain.service.InventoryService;

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
    return getFirstItem(servicePointsUsersClient.findServicePointsUsers(userId))
      .map(ServicePointUserDTO::getDefaultServicePointId);
  }

  @Override
  public Optional<UUID> findServicePointIdByCode(String code) {
    return getFirstItem(servicePointsClient.queryServicePointByCode(code))
      .map(ServicePointsClient.ServicePoint::getId);
  }

  @Override
  public InstanceTypeClient.InstanceType queryInstanceTypeByName(String name) {
    return getFirstItem(instanceTypeClient.queryInstanceTypeByName(name))
      .orElseThrow(() -> new IllegalArgumentException("Instance type is not found by name: " + name));
  }

  @Override
  public InstanceContributorTypeClient.NameType queryContributorTypeByName(String name) {
    return getFirstItem(nameTypeClient.queryContributorTypeByName(name))
      .orElseThrow(() -> new IllegalArgumentException("Contributor name type is not found by name: " + name));
  }

  @Override
  public HridSettingsClient.HridSettings getHridSettings() {
    return hridSettingsClient.getHridSettings();
  }

}
