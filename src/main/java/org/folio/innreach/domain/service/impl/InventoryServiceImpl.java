package org.folio.innreach.domain.service.impl;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.InventoryViewClient;
import org.folio.innreach.client.InventoryViewClient.InstanceView;
import org.folio.innreach.domain.service.InventoryService;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@RequiredArgsConstructor
@Service
public class InventoryServiceImpl implements InventoryService {

  private final InventoryViewClient inventoryViewClient;

  @Override
  public Instance getInstance(UUID instanceId) {
    return inventoryViewClient.getInstanceById(instanceId)
      .getResult()
      .stream()
      .findFirst()
      .map(InstanceView::toInstance)
      .orElseThrow(() -> new IllegalArgumentException("No inventory instance found for id = " + instanceId));
  }

  @Override
  public List<Item> getItemsByInstanceId(UUID instanceId) {
    return getInstance(instanceId).getItems();
  }

}
