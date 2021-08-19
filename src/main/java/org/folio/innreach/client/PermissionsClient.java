package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;

@FeignClient(name = "perms/users", configuration = FolioFeignClientConfig.class)
public interface PermissionsClient {

  @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  void assignPermissionsToUser(@RequestBody Permissions permissions);

  @PostMapping(value = "/{userId}/permissions?indexField=userId", consumes = APPLICATION_JSON_VALUE)
  void addPermission(@PathVariable("userId") String userId, Permission permission);

  @GetMapping(value = "/{userId}/permissions?indexField=userId")
  ResultList<String> getUserPermissions(@PathVariable("userId") String userId);

  @Data
  @AllArgsConstructor(staticName = "of")
  @NoArgsConstructor
  class Permission {
    private String permissionName;
  }

  @Data
  @AllArgsConstructor(staticName = "of")
  @NoArgsConstructor
  class Permissions {
    private String id;
    private String userId;
    @JsonProperty("permissions")
    private List<String> allowedPermissions = Collections.emptyList();
  }
}
