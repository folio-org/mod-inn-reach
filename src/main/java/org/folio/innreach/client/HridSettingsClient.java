package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;
import lombok.Data;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("hrid-settings-storage")
public interface HridSettingsClient {

  @GetExchange(value = "hrid-settings", accept = APPLICATION_JSON_VALUE)
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
