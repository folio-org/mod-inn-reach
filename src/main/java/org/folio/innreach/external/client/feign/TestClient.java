// TODO Remove this class after testing is complete.
package org.folio.innreach.external.client.feign;

import java.net.URI;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import org.folio.innreach.external.client.feign.config.InnReachFeignClientConfig;
import org.springframework.web.bind.annotation.RequestBody;

// TODO Simulates calls to outside services. Although it uses the inn reach client config it can be used to simulate calls to folio too.
// TODO The important thing is that requests flow through okhttp in the same way as they do in prod.
@FeignClient(value = "testClient", configuration = InnReachFeignClientConfig.class)
public interface TestClient {

  @PostMapping(value = "/test")
  String makeTestRequest(URI localhostServerUrl, @RequestBody String anything);
}
