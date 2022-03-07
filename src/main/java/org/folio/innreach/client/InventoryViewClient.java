package org.folio.innreach.client;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@FeignClient(name = "inventory-view", configuration = FolioFeignClientConfig.class)
public interface InventoryViewClient {

  @GetMapping(path = "/instances?query=(id={instanceId})&limit=1", consumes = "binary/octet-stream")
  ResultList<InstanceView> getInstanceById(@PathVariable("instanceId") UUID instanceId);

  @GetMapping(path = "/instances?query=(instance.hrid={hrid})&limit=1", consumes = "binary/octet-stream")
  ResultList<InstanceView> getInstanceByHrid(@PathVariable("hrid") String instanceHrid);

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  class InstanceView {
    private Instance instance;
    private List<Holding> holdingsRecords = emptyList();
    private List<Item> items = emptyList();

    public Instance toInstance() {
      return instance.holdingsRecords(holdingsRecords).items(items);
    }
  }
}
