package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import org.folio.innreach.config.FolioFeignClientConfig;

@FeignClient(value = "hrid-settings-storage", configuration = FolioFeignClientConfig.class)
public interface HridSettingsClient {

  @GetMapping(value = "hrid-settings", produces = APPLICATION_JSON_VALUE)
  HridSettings getHridSettings();

  @Data
  class HridSettings {
    private UUID id;
    private HridSetting instances;
    private HridSetting holdings;
    private HridSetting items;
    private Boolean commonRetainLeadingZeroes;
  }

  @Data
  class HridSetting {
    private String prefix;
    private Integer startNumber;
  }

}
