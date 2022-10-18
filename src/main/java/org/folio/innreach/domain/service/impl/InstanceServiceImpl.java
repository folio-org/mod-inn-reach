package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.util.CqlHelper.matchAny;
import static org.folio.innreach.util.ListUtils.getFirstItem;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.service.InstanceService;
@Log4j2
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
  public void delete(UUID instanceId) {
    inventoryClient.findInstance(instanceId)
      .ifPresentOrElse(instance-> inventoryClient.deleteInstance(instanceId)
        ,()->log.info("Instance not found with instanceId:"+instanceId));
    log.info("Instance deleted-->>"+instanceId);
  }

  @Override
  public List<InventoryInstanceDTO> findInstancesByIds(Set<UUID> instanceIds, int limit) {
    return inventoryClient.queryInstancesByIds(matchAny(instanceIds), limit).getResult();
  }

  @Override
  public String getAuthor(InventoryInstanceDTO instance) {
    return instance.getContributors().stream()
      .filter(InventoryInstanceDTO.ContributorDTO::getPrimary)
      .map(InventoryInstanceDTO.ContributorDTO::getName)
      .findFirst()
      .orElse(null);
  }

  @Override
  public InventoryInstanceDTO update(InventoryInstanceDTO inventoryInstanceDTO) {
    throw new UnsupportedOperationException();
  }

}
