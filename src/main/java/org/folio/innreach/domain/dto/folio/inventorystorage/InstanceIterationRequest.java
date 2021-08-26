package org.folio.innreach.domain.dto.folio.inventorystorage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class InstanceIterationRequest {

  private String eventType;
  private String topicName;

}
