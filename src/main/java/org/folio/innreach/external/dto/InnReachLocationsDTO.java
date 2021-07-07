package org.folio.innreach.external.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InnReachLocationsDTO {

  private String status;
  private String reason;
  private List<InnReachLocationDTO> locationList;
  private List<String> errors;

  public InnReachLocationsDTO(List<InnReachLocationDTO> locationList) {
    this.locationList = locationList;
  }
}
