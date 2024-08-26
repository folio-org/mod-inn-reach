package org.folio.innreach.domain.dto.folio;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.innreach.domain.dto.deserializer.CustomFieldsDeserializer;

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

  @JsonDeserialize(using = CustomFieldsDeserializer.class)
  private Map<String, String> customFields;

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
