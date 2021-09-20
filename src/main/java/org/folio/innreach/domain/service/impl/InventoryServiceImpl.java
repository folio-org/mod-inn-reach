package org.folio.innreach.domain.service.impl;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.InventoryViewClient;
import org.folio.innreach.client.InventoryViewClient.InstanceView;
import org.folio.innreach.domain.service.InventoryService;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@Log4j2
@RequiredArgsConstructor
@Service
public class InventoryServiceImpl implements InventoryService {

  private final InventoryViewClient inventoryViewClient;

  @Override
  public Instance getInstance(UUID instanceId) {
    try {
      return inventoryViewClient.getInstanceById(instanceId)
        .getResult()
        .stream()
        .findFirst()
        .map(InstanceView::toInstance)
        .orElse(null);
    } catch (Exception e) {
      log.warn("Unable to load instance by id {}", instanceId, e);
    }
    return null;
  }

  @Override
  public List<Item> getItemsByInstanceId(UUID instanceId) {
    return getInstance(instanceId).getItems();
  }

}
