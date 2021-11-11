package org.folio.innreach.domain.service;

import java.util.Optional;
import java.util.UUID;

import org.folio.innreach.client.HridSettingsClient;
import org.folio.innreach.client.InstanceContributorTypeClient;
import org.folio.innreach.client.InstanceTypeClient;
import org.folio.innreach.client.ServicePointsClient;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.dto.Holding;


public interface InventoryService {

  InventoryInstanceDTO queryInstanceByHrid(String instanceHrid);

  ServicePointsClient.ServicePoint queryServicePointByCode(String locationCode);

  InstanceTypeClient.InstanceType queryInstanceTypeByName(String name);

  InstanceContributorTypeClient.NameType queryContributorTypeByName(String name);

  HridSettingsClient.HridSettings getHridSettings();

  InventoryInstanceDTO createInstance(InventoryInstanceDTO instance);

  Holding createHolding(Holding holding);

  InventoryItemDTO createItem(InventoryItemDTO item);

  InventoryItemDTO updateItem(InventoryItemDTO item);

  Optional<InventoryItemDTO> findItem(UUID itemId);

  InventoryItemDTO getItemByHrId(String hrid);

  InventoryItemDTO getItemByBarcode(String barcode);

  Optional<Holding> findHolding(UUID holdingId);

  Holding updateHolding(Holding holding);
}
