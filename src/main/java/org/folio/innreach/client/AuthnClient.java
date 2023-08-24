package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.folio.innreach.client.config.FolioFeignClientConfig;

@FeignClient(name = "test", configuration = FolioFeignClientConfig.class)
public interface AuthnClient {

  @PostMapping(value = "/login-with-expiry", consumes = APPLICATION_JSON_VALUE)
  ResponseEntity<LoginResponse> loginWithExpiry(@RequestBody UserCredentials credentials);

  @PostMapping(value = "/login", consumes = APPLICATION_JSON_VALUE)
  ResponseEntity<LoginResponse> login(@RequestBody UserCredentials credentials);

  @PostMapping(value = "/credentials", consumes = APPLICATION_JSON_VALUE)
  void saveCredentials(@RequestBody UserCredentials credentials);

  @Data
  @AllArgsConstructor(staticName = "of")
  class UserCredentials {
    private String username;
    private String password;
  }

  record LoginResponse(String accessTokenExpiration) {
  }
}
