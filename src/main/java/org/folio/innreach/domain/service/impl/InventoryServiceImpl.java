package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.util.ListUtils.getFirstItem;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.HridSettingsClient;
import org.folio.innreach.client.InstanceContributorTypeClient;
import org.folio.innreach.client.InstanceTypeClient;
import org.folio.innreach.client.ServicePointsClient;
import org.folio.innreach.domain.service.InventoryService;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

  private final ServicePointsClient servicePointsClient;
  private final HridSettingsClient hridSettingsClient;
  private final InstanceTypeClient instanceTypeClient;
  private final InstanceContributorTypeClient nameTypeClient;


  @Override
  public ServicePointsClient.ServicePoint queryServicePointByCode(String locationCode) {
    return getFirstItem(servicePointsClient.queryServicePointByCode(locationCode))
      .orElseThrow(() -> new IllegalArgumentException("Service point is not found for pickup location code: " + locationCode));
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
