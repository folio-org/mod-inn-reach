package org.folio.innreach.domain.dto.folio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationDTO {

  private String id;
  private String module;
  private String configName;
  private String code;
  private String description;
  private String value;

}
