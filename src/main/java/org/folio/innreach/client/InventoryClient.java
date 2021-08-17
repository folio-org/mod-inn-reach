package org.folio.innreach.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.dto.folio.inventory.MaterialTypeDTO;

@FeignClient("inventory")
public interface InventoryClient {

  @GetMapping("/inventory/instances/{id}")
  InventoryInstanceDTO getInstanceById(@PathVariable("id") UUID instanceId);

  @GetMapping("/material-types")
  ResultList<MaterialTypeDTO> getMaterialTypes(@RequestParam("query") String query, @RequestParam("limit") int limit);

}
