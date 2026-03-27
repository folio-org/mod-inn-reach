package org.folio.innreach.domain.dto.folio.circulation;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CirculationSettingDTO {

  private String id;
  private String name;
  private Map<String, Object> value;

}
