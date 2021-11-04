package org.folio.innreach.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.dto.Item;

@FeignClient(name = "item-storage", configuration = FolioFeignClientConfig.class)
public interface ItemsStorageClient {

  @PostMapping("/items")
  Item createItem(@RequestBody Item item);

}
