package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.util.ListUtils.getFirstItem;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.HoldingsStorageClient;
import org.folio.innreach.client.HridSettingsClient;
import org.folio.innreach.client.InstanceContributorTypeClient;
import org.folio.innreach.client.InstanceTypeClient;
import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.client.ServicePointsClient;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.service.InventoryService;
import org.folio.innreach.dto.Holding;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

  private final InventoryClient inventoryClient;
  private final HoldingsStorageClient holdingsStorageClient;
  private final ServicePointsClient servicePointsClient;
  private final HridSettingsClient hridSettingsClient;
  private final InstanceTypeClient instanceTypeClient;
  private final InstanceContributorTypeClient nameTypeClient;

  @Override
  public InventoryInstanceDTO queryInstanceByHrid(String instanceHrid) {
    return getFirstItem(inventoryClient.queryInstanceByHrid(instanceHrid))
      .orElseThrow(() -> new IllegalArgumentException("No instance found by hrid " + instanceHrid));
  }

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

  @Override
  public InventoryInstanceDTO createInstance(InventoryInstanceDTO instance) {
    inventoryClient.createInstance(instance);
    return getFirstItem(inventoryClient.queryInstanceByHrid(instance.getHrid()))
      .orElseThrow(() -> new IllegalArgumentException("Can't create instance with hrid: " + instance.getHrid()));
  }

  @Override
  public Holding createHolding(Holding holding) {
    return holdingsStorageClient.createHolding(holding);
  }

  @Override
  public InventoryItemDTO createItem(InventoryItemDTO item) {
    return inventoryClient.createItem(item);
  }

  @Override
  public InventoryItemDTO updateItem(InventoryItemDTO item) {
    return inventoryClient.updateItem(item.getId(), item);
  }

  @Override
  public Optional<InventoryItemDTO> findItem(UUID itemId) {
    return inventoryClient.findItem(itemId);
  }

  @Override
  public InventoryItemDTO getItemByHrId(String hrid) {
    return getFirstItem(inventoryClient.getItemsByHrId(hrid))
      .orElseThrow(() -> new IllegalArgumentException("Item with hrid = " + hrid + " not found."));
  }

  @Override
  public Optional<Holding> findHolding(UUID holdingId) {
    return holdingsStorageClient.findHolding(holdingId);
  }

  @Override
  public Holding updateHolding(Holding holding) {
    return holdingsStorageClient.updateHolding(holding.getId(), holding);
  }

}
