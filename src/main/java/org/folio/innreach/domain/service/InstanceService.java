package org.folio.innreach.domain.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;

public interface InstanceService extends BasicService<UUID, InventoryInstanceDTO> {

  InventoryInstanceDTO queryInstanceByHrid(String instanceHrid);

  List<InventoryInstanceDTO> findInstancesByIds(Set<UUID> instanceIds, int limit);

  String getAuthor(InventoryInstanceDTO instance);
}
