package org.folio.innreach.domain.event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "data")
public class DomainEvent<T> {
  protected UUID recordId;
  protected DomainEventType type;
  protected String tenant;
  protected long timestamp;
  protected EntityChangedData<T> data;
}
