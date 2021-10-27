package org.folio.innreach.domain.service;

import org.folio.innreach.client.HridSettingsClient;
import org.folio.innreach.client.InstanceContributorTypeClient;
import org.folio.innreach.client.InstanceTypeClient;
import org.folio.innreach.client.ServicePointsClient;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;


public interface InventoryStorageService {

  Instance queryInstanceByHrid(String instanceHrid);

  ServicePointsClient.ServicePoint queryServicePointByCode(String locationCode);

  InstanceTypeClient.InstanceType queryInstanceTypeByName(String name);

  InstanceContributorTypeClient.NameType queryContributorTypeByName(String name);

  HridSettingsClient.HridSettings getHridSettings();

  Instance createInstance(Instance instance);

  Holding createHolding(Holding holding);

  Item createItem(Item item);

}
