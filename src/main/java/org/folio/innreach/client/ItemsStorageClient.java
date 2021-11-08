package org.folio.innreach.client;

import java.util.Optional;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.dto.Item;

@FeignClient(name = "item-storage", configuration = FolioFeignClientConfig.class)
public interface ItemsStorageClient {

  @PostMapping("/items")
  Item createItem(@RequestBody Item item);

  @PutMapping("/items/{itemId}")
  Item updateItem(@PathVariable("itemId") UUID itemId, @RequestBody Item item);

  @GetMapping("/items/{itemId}")
  Optional<Item> findItem(@PathVariable("itemId") UUID itemId);
}
