package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.client.cql.CqlQuery.exactMatchAny;

import java.util.Collections;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.InventoryViewClient;
import org.folio.innreach.client.InventoryViewClient.InstanceView;
import org.folio.innreach.domain.service.InventoryService;
import org.folio.innreach.dto.Instance;

@RequiredArgsConstructor
@Service
public class InventoryServiceImpl implements InventoryService {

  private final InventoryViewClient inventoryViewClient;

  @Override
  public Instance getInstance(UUID instanceId) {
    return inventoryViewClient.getInstances(exactMatchAny("id", Collections.singleton(instanceId.toString())), 1)
      .getResult()
      .stream()
      .findFirst()
      .map(InstanceView::toInstance)
      .orElseThrow(() -> new IllegalArgumentException("No inventory instance found for id = " + instanceId));
  }

}
