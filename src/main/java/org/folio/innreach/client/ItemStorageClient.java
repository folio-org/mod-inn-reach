package org.folio.innreach.client;

import java.util.Optional;
import java.util.UUID;

import org.folio.innreach.domain.dto.folio.ResultList;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import org.folio.innreach.dto.Item;

@HttpExchange("item-storage")
public interface ItemStorageClient {

  @GetExchange("/items/{itemId}")
  Optional<Item> getItemById(@PathVariable("itemId") UUID itemId);

  @GetExchange("/items")
  ResultList<Item> findByQuery(@RequestParam("query") String query);

}
