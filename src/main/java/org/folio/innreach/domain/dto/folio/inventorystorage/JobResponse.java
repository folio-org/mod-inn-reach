package org.folio.innreach.domain.dto.folio.inventorystorage;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JobResponse {
  private UUID id;
  @JsonProperty("messagesPublished")
  private Integer numberOfRecordsPublished;
  @JsonProperty("jobStatus")
  private JobStatus status;
  private OffsetDateTime submittedDate;

  @JsonSetter("submittedDate")
  public void submittedDateFromString(String value){
    submittedDate = OffsetDateTime.parse(value);
  }

  @AllArgsConstructor
  public enum JobStatus {
    IN_PROGRESS("In progress"),
    FAILED("Failed"),
    COMPLETED("Completed"),
    CANCELLATION_PENDING("Cancellation pending"),
    CANCELLED("Cancelled");

    @Getter
    @JsonValue
    private String value;
  }
}
