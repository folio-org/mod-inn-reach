package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.util.ListUtils.getFirstItem;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.service.InstanceService;

@Service
@RequiredArgsConstructor
public class InstanceServiceImpl implements InstanceService {

  private final InventoryClient inventoryClient;

  @Override
  public InventoryInstanceDTO queryInstanceByHrid(String instanceHrid) {
    return getFirstItem(inventoryClient.queryInstanceByHrid(instanceHrid))
        .orElseThrow(() -> new IllegalArgumentException("No instance found by hrid " + instanceHrid));
  }

  @Override
  public InventoryInstanceDTO create(InventoryInstanceDTO instance) {
    inventoryClient.createInstance(instance);
    return getFirstItem(inventoryClient.queryInstanceByHrid(instance.getHrid()))
        .orElseThrow(() -> new IllegalArgumentException("Can't create instance with hrid: " + instance.getHrid()));
  }

  @Override
  public Optional<InventoryInstanceDTO> find(UUID uuid) {
    return inventoryClient.findInstance(uuid);
  }

  @Override
  public InventoryInstanceDTO update(InventoryInstanceDTO inventoryInstanceDTO) {
    throw new UnsupportedOperationException();
  }

}
