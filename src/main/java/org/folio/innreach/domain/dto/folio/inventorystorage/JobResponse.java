package org.folio.innreach.domain.dto.folio.inventorystorage;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
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
  private JobStatus status;
  private OffsetDateTime submittedDate;

  @AllArgsConstructor
  public enum JobStatus {
    IN_PROGRESS("In progress"),
    FAILED("Failed"),
    COMPLETED("Completed"),
    CANCELLATION_PENDING("Cancellation pending"),
    CANCELLED("Cancelled");

    @Getter
    private String value;
  }
}
