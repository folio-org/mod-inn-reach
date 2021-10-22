package org.folio.innreach.external.service.impl;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.external.service.InventoryService;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class InventoryServiceImpl implements InventoryService {

  private final InventoryClient client;

  @Override
  public InventoryItemDTO getItemByHrId(String hrid) {
    return client.getItemsByHrId(hrid).getResult()
      .stream().findFirst().orElseThrow( () -> new EntityNotFoundException("Item with hrid = " + hrid + " not found."));
  }
}
