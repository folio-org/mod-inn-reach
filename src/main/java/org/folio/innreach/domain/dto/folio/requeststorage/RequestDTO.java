package org.folio.innreach.domain.dto.folio.requeststorage;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestDTO {
  private UUID id;
  private UUID itemId;
  private RequestStatus status;
  private String requestType;
  private OffsetDateTime requestExpirationDate;
  private UUID requesterId;
  private UUID pickupServicePointId;
  private String patronComments;
  private OffsetDateTime requestDate;
  private String fulfilmentPreference;
  private UUID cancellationReasonId;
  private String cancellationAdditionalInformation;

  @Getter
  @RequiredArgsConstructor
  public enum RequestStatus {
    OPEN_NOT_YET_FILLED("Open - Not yet filled"),
    OPEN_AWAITING_PICKUP("Open - Awaiting pickup"),
    OPEN_IN_TRANSIT("Open - In transit"),
    OPEN_AWAITING_DELIVERY("Open - Awaiting delivery"),
    CLOSED_FILLER("Closed - Filled"),
    CLOSED_CANCELLED("Closed - Cancelled"),
    CLOSED_UNFILLED("Closed - Unfilled"),
    CLOSED_PICKUP_EXPIRED("Closed - Pickup expired");

    private final String name;
  }

  @Getter
  @RequiredArgsConstructor
  public enum RequestType {
    HOLD("Hold"),
    RECALL("Recall"),
    PAGE("Page");

    private final String name;
  }

  @Getter
  @RequiredArgsConstructor
  public enum FulfilmentPreference {
    HOLD_SHELF("Hold Shelf"),
    DELIVERY("Delivery");

    private final String name;
  }
}
