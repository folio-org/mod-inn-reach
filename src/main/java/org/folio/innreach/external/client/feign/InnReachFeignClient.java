package org.folio.innreach.external.client.feign;

import org.folio.innreach.external.dto.AccessTokenDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.net.URI;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@FeignClient(value = "innReach")
public interface InnReachFeignClient {

  @PostMapping("/auth/v1/oauth2/token?grant_type=client_credentials&scope=innreach_tp")
  ResponseEntity<AccessTokenDTO> getAccessToken(URI baseUri, @RequestHeader(AUTHORIZATION) String authorizationHeader);
}
