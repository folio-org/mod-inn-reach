package org.folio.innreach.domain.service;

import java.util.Optional;
import java.util.UUID;

import org.folio.innreach.client.HridSettingsClient;
import org.folio.innreach.client.InstanceContributorTypeClient;
import org.folio.innreach.client.InstanceTypeClient;

public interface InventoryService {

  Optional<UUID> findDefaultServicePointIdForUser(UUID userId);

  Optional<UUID> findServicePointIdByCode(String locationCode);

  InstanceTypeClient.InstanceType queryInstanceTypeByName(String name);

  InstanceContributorTypeClient.NameType queryContributorTypeByName(String name);

  HridSettingsClient.HridSettings getHridSettings();

}
