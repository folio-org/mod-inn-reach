package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;

public interface InstanceService extends BasicService<UUID, InventoryInstanceDTO>,
                                          RetryableUpdateTemplate<UUID, InventoryInstanceDTO> {

  InventoryInstanceDTO queryInstanceByHrid(String instanceHrid);

}
