package org.folio.innreach.domain.service.impl;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.InventoryViewClient;
import org.folio.innreach.client.InventoryViewClient.InstanceView;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.dto.Instance;

@Log4j2
@RequiredArgsConstructor
@Service
public class InventoryViewServiceImpl implements InventoryViewService {

  private final InventoryViewClient inventoryViewClient;

  @Override
  public Instance getInstance(UUID instanceId) {
    log.debug("getInstance:: parameters instanceId: {}", instanceId);
    return fetchInstance(() -> inventoryViewClient.getInstanceById(instanceId))
      .orElseThrow(() -> new IllegalArgumentException("Unable to load inventory-view by instance id: " + instanceId));
  }

  @Override
  public Instance getInstanceByHrid(String instanceHrid) {
    log.debug("getInstanceByHrid:: parameters instanceHrid: {}", instanceHrid);
    return fetchInstance(() -> inventoryViewClient.getInstanceByHrid(instanceHrid))
      .orElseThrow(() -> new IllegalArgumentException("Unable to load inventory-view by instance hrid: " + instanceHrid));
  }

  private Optional<Instance> fetchInstance(Supplier<ResultList<InstanceView>> instanceViewSupplier) {
    log.debug("fetchInstance:: parameters instanceViewSupplier: {}", instanceViewSupplier);
    return instanceViewSupplier.get()
      .getResult()
      .stream()
      .findFirst()
      .map(InstanceView::toInstance);
  }

}
