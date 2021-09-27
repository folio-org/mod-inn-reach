package org.folio.innreach.domain.service.impl;

import java.util.UUID;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.InventoryViewClient;
import org.folio.innreach.client.InventoryViewClient.InstanceView;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.InventoryService;
import org.folio.innreach.dto.Instance;

@Log4j2
@RequiredArgsConstructor
@Service
public class InventoryServiceImpl implements InventoryService {

  private final InventoryViewClient inventoryViewClient;

  @Override
  public Instance getInstance(UUID instanceId) {
    try {
      return fetchInstance(() -> inventoryViewClient.getInstanceById(instanceId));
    } catch (Exception e) {
      log.warn("Inventory instance with id {} not found", instanceId, e);
    }
    return null;
  }

  @Override
  public Instance getInstanceByHrid(String instanceHrid) {
    try {
      return fetchInstance(() -> inventoryViewClient.getInstanceByHrid(instanceHrid));
    } catch (Exception e) {
      throw new RuntimeException("Unable to load inventory instance with hrid " + instanceHrid);
    }
  }

  private Instance fetchInstance(Supplier<ResultList<InstanceView>> instanceViewSupplier) {
    return instanceViewSupplier.get()
      .getResult()
      .stream()
      .findFirst()
      .map(InstanceView::toInstance)
      .orElse(null);
  }

}
