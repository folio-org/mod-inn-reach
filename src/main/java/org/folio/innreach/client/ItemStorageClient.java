package org.folio.innreach.client;

import java.util.Optional;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.dto.Item;

@FeignClient(name = "item-storage", configuration = FolioFeignClientConfig.class, dismiss404 = true)
public interface ItemStorageClient {

  @GetMapping("/items/{itemId}")
  Optional<Item> getItemById(@PathVariable("itemId") UUID itemId);

}
