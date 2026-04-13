package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Optional;
import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.User;

@HttpExchange("users")
public interface UsersClient {

  @GetExchange(value = "/{id}")
  Optional<User> getUserById(@PathVariable("id") UUID id);

  @GetExchange
  ResultList<User> findByQuery(@RequestParam("query") String query);

  @PostExchange(contentType = APPLICATION_JSON_VALUE)
  void saveUser(@RequestBody User user);

}
