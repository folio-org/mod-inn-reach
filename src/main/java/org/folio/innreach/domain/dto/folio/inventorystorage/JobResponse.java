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
  @JsonProperty("published")
  private Integer numberOfRecordsPublished;
  private JobStatus status;
  private OffsetDateTime submittedDate;

  @AllArgsConstructor
  public enum JobStatus {
    IN_PROGRESS("In progress"),
    ID_PUBLISHING_FAILED("Id publishing failed"),
    IDS_PUBLISHED("Ids published"),
    PENDING_CANCEL("Pending cancel"),
    ID_PUBLISHING_CANCELLED("Id publishing cancelled");

    @Getter
    private String value;
  }
}
