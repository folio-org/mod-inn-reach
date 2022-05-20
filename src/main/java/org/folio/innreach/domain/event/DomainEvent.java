package org.folio.innreach.domain.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
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
  protected DomainEventType type;
  protected String tenant;
  protected Long timestamp;
  protected EntityChangedData<T> data;

  @JsonSetter("old")
  public void setOldEntity(T oldEntity) {
    if (data != null) {
      data.setOldEntity(oldEntity);
    } else {
      data = new EntityChangedData<>(oldEntity, null);
    }
  }

  @JsonSetter("new")
  public void setNewEntity(T newEntity) {
    if (data != null) {
      data.setNewEntity(newEntity);
    } else {
      data = new EntityChangedData<>(null, newEntity);
    }
  }

}
