package org.folio.innreach.client;

import static java.util.Collections.emptyList;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@FeignClient(name = "inventory-view", configuration = FolioFeignClientConfig.class)
public interface InventoryViewClient {

  @GetMapping(path = "/instances?query=(id==\"{id}\")&limit=1", consumes = APPLICATION_OCTET_STREAM_VALUE)
  ResultList<InstanceView> getInstanceById(@PathVariable("id") UUID instanceId);

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  class InstanceView {
    private Instance instance;
    private List<Item> items = emptyList();

    public Instance toInstance() {
      return instance.items(items);
    }
  }
}
