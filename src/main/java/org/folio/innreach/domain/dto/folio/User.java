package org.folio.innreach.domain.dto.folio;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

  private UUID id;
  private String externalSystemId;
  private String username;
  @JsonProperty("patronGroup")
  private UUID patronGroupId;
  private OffsetDateTime expirationDate;
  private boolean active;
  private Personal personal;
  private String barcode;
  private Map<String, ArrayList<String>> customFields;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor(staticName = "of")
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Personal {
    private String firstName;
    private String middleName;
    private String lastName;
    private String preferredFirstName;
  }

}
