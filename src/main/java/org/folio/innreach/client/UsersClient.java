package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.User;

@FeignClient(name = "users", contextId = "userClient")
public interface UsersClient {

  @GetMapping(value = "/{id}")
  Optional<org.folio.innreach.domain.dto.folio.User> getUserById(@PathVariable("id") UUID id);

  @GetMapping
  ResultList<org.folio.innreach.domain.dto.folio.User> query(@RequestParam("query") String query);

  @GetMapping
  org.folio.spring.model.ResultList<User> query1(@RequestParam("query") String query);

  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  void saveUser(@RequestBody org.folio.innreach.domain.dto.folio.User user);

  public static record User(String id, String username, boolean active, org.folio.spring.client.UsersClient.User.Personal personal) {
    public User(String id, String username, boolean active, org.folio.spring.client.UsersClient.User.Personal personal) {
      this.id = id;
      this.username = username;
      this.active = active;
      this.personal = personal;
    }

    public String id() {
      return this.id;
    }

    public String username() {
      return this.username;
    }

    public boolean active() {
      return this.active;
    }

    public org.folio.spring.client.UsersClient.User.Personal personal() {
      return this.personal;
    }

    @JsonIgnoreProperties(
      ignoreUnknown = true
    )
    public static record Personal(String firstName, String lastName) {
      public Personal(String lastName) {
        this((String)null, lastName);
      }

      public Personal(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
      }

      public String firstName() {
        return this.firstName;
      }

      public String lastName() {
        return this.lastName;
      }
    }
  }


}
