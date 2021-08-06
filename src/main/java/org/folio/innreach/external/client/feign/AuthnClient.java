package org.folio.innreach.external.client.feign;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.folio.innreach.external.client.feign.config.FolioFeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "authn", configuration = FolioFeignClientConfig.class)
public interface AuthnClient {

  @PostMapping(value = "/login", consumes = APPLICATION_JSON_VALUE)
  ResponseEntity<String> getApiKey(@RequestBody UserCredentials credentials);

  @PostMapping(value = "/credentials", consumes = APPLICATION_JSON_VALUE)
  void saveCredentials(@RequestBody UserCredentials credentials);

  @Data
  @AllArgsConstructor(staticName = "of")
  class UserCredentials {
    private String username;
    private String password;
  }
}
