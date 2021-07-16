package org.folio.innreach.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InnReachLocationDTO {

  @JsonProperty("locationKey")
  private String code;
  private String description;
}
