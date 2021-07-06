package org.folio.innreach.external.client.feign;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.net.URI;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import org.folio.innreach.external.client.feign.config.InnReachFeignClientConfig;
import org.folio.innreach.external.dto.AccessTokenDTO;

@FeignClient(value = "innReachAuth", configuration = InnReachFeignClientConfig.class)
public interface InnReachAuthClient {

  @PostMapping("/auth/v1/oauth2/token?grant_type=client_credentials&scope=innreach_tp")
  ResponseEntity<AccessTokenDTO> getAccessToken(URI baseUri, @RequestHeader(AUTHORIZATION) String authorizationHeader);
}
