package org.folio.innreach.domain.dto.folio.inventory;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryInstanceDTO {

  private UUID id;
  @JsonProperty("_version")
  private String version;
  private String hrid;
  private UUID instanceTypeId;
  private String title;
  private String source;
  private Boolean staffSuppress;
  private Boolean discoverySuppress;
  private List<IdentifierDTO> identifiers;
  private List<ContributorDTO> contributors;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class IdentifierDTO {

    @JsonProperty("identifierTypeId")
    private UUID id;
    private String value;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ContributorDTO {
    private UUID contributorNameTypeId;
    private String name;
  }
}
