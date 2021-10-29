package org.folio.innreach.domain.dto.folio.inventory;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
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
  private String hrId;
  private String barcode;

  @JsonUnwrapped
  private InventoryItemStatus status;
  private MaterialType materialType;
  private PermanentLoanType permanentLoanType;
  private TemporaryLoanType temporaryLoanType;
  private PermanentLocation permanentLocation;

  @Data
  public static class MaterialType {
    private final UUID id;
    private final String name;
  }

  @Data
  public static class PermanentLoanType {
    private final UUID id;
    private final String name;
  }

  @Data
  public static class TemporaryLoanType {
    private final UUID id;
    private final String name;
  }

  @Data
  public static class PermanentLocation {
    private final UUID id;
    private final String name;
  }

}
