package org.folio.innreach.domain.event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@ToString(exclude = "data")
public class DomainEvent<T> {
  UUID recordId;
  DomainEventType type;
  String tenant;
  long timestamp;
  T data;
}
