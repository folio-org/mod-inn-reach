package org.folio.innreach.domain.service;

import org.folio.innreach.client.HridSettingsClient;
import org.folio.innreach.client.InstanceContributorTypeClient;
import org.folio.innreach.client.InstanceTypeClient;
import org.folio.innreach.client.ServicePointsClient;

public interface InventoryService {

  ServicePointsClient.ServicePoint queryServicePointByCode(String locationCode);

  InstanceTypeClient.InstanceType queryInstanceTypeByName(String name);

  InstanceContributorTypeClient.NameType queryContributorTypeByName(String name);

  HridSettingsClient.HridSettings getHridSettings();

}
