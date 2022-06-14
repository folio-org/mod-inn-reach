package org.folio.innreach.domain.dto.folio.inventory;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemDTO {

  private UUID id;
  @JsonProperty("_version")
  private String version;
  private String hrid;
  private String barcode;
  private String callNumber;
  private UUID holdingsRecordId;
  private InventoryItemStatus status;
  private MaterialType materialType;
  private LoanType permanentLoanType;
  private LoanType temporaryLoanType;
  private Location permanentLocation;
  private Location effectiveLocation;
  private Boolean discoverySuppress;
  private String title;
  private String effectiveShelvingOrder;
  private String author;

  @Data
  public static class MaterialType {
    private final UUID id;
    private final String name;
  }

  @Data
  public static class LoanType {
    private final UUID id;
    private final String name;
  }

  @Data
  public static class Location {
    private final UUID id;
    private final String name;
  }

}
