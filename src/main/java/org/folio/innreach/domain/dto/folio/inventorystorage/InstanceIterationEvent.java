package org.folio.innreach.domain.dto.folio.inventorystorage;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class InstanceIterationEvent {

  private UUID jobId;
  private String type;
  private String tenant;
  private UUID instanceId;

}
