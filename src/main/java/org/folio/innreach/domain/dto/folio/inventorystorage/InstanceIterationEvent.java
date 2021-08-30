package org.folio.innreach.domain.dto.folio.inventorystorage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class InstanceIterationEvent {

  private String jobId;
  private String type;
  private String tenant;
  private String instanceId;

}
