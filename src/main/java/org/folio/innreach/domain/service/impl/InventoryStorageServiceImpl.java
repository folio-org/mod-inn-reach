package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.util.ListUtils.getFirstItem;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.HoldingsStorageClient;
import org.folio.innreach.client.HridSettingsClient;
import org.folio.innreach.client.InstanceContributorTypeClient;
import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.client.InstanceTypeClient;
import org.folio.innreach.client.ItemsStorageClient;
import org.folio.innreach.client.ServicePointsClient;
import org.folio.innreach.domain.service.InventoryStorageService;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@Service
@RequiredArgsConstructor
public class InventoryStorageServiceImpl implements InventoryStorageService {

  private final InstanceStorageClient instanceStorageClient;
  private final ItemsStorageClient itemsStorageClient;
  private final HoldingsStorageClient holdingsStorageClient;
  private final ServicePointsClient servicePointsClient;
  private final HridSettingsClient hridSettingsClient;
  private final InstanceTypeClient instanceTypeClient;
  private final InstanceContributorTypeClient nameTypeClient;

  @Override
  public Instance queryInstanceByHrid(String instanceHrid) {
    return getFirstItem(instanceStorageClient.queryInstanceByHrid(instanceHrid))
      .orElseThrow(() -> new IllegalStateException("No instance found by hrid " + instanceHrid));
  }

  @Override
  public ServicePointsClient.ServicePoint queryServicePointByCode(String locationCode) {
    return getFirstItem(servicePointsClient.queryServicePointByCode(locationCode))
      .orElseThrow(() -> new IllegalStateException("Service point is not found for pickup location code: " + locationCode));
  }

  @Override
  public InstanceTypeClient.InstanceType queryInstanceTypeByName(String name) {
    return getFirstItem(instanceTypeClient.queryInstanceTypeByName(name))
      .orElseThrow(() -> new IllegalStateException("Instance type is not found by name: " + name));
  }

  @Override
  public InstanceContributorTypeClient.NameType queryContributorTypeByName(String name) {
    return getFirstItem(nameTypeClient.queryContributorTypeByName(name))
      .orElseThrow(() -> new IllegalStateException("Contributor name type is not found by name: " + name));
  }

  @Override
  public HridSettingsClient.HridSettings getHridSettings() {
    return hridSettingsClient.getHridSettings();
  }

  @Override
  public Instance createInstance(Instance instance) {
    return instanceStorageClient.createInstance(instance);
  }

  @Override
  public Holding createHolding(Holding holding) {
    return holdingsStorageClient.createHolding(holding);
  }

  @Override
  public Item createItem(Item item) {
    return itemsStorageClient.createItem(item);
  }

  @Override
  public Item updateItem(Item item) {
    return itemsStorageClient.updateItem(item.getId(), item);
  }

  @Override
  public Optional<Item> findItem(UUID itemId) {
    return itemsStorageClient.findItem(itemId);
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
