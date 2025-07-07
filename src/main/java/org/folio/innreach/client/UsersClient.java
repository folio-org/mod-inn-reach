package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Optional;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.User;

@FeignClient(name = "users", configuration = FolioFeignClientConfig.class, dismiss404 = true, contextId = "usersClientInnReach")
public interface UsersClient {

  @GetMapping(value = "/{id}")
  Optional<User> getUserById(@PathVariable("id") UUID id);

  @GetMapping
  ResultList<User> query(@RequestParam("query") String query);

  @GetMapping(path = "?query=barcode=={barcode}", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
  ResultList<User> queryUsersByBarcode(@PathVariable("barcode") String barcode);

  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  void saveUser(@RequestBody User user);

}
