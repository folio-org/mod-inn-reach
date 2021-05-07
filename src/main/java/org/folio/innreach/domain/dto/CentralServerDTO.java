package org.folio.innreach.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class CentralServerDTO {
  private UUID id;

  @NotEmpty(message = "name is required")
  private String name;
  private String description;

  @NotEmpty(message = "localServerCode is required")
  @Size(min = 5, max = 5, message = "localServerCode must be 5 characters long")
  private String localServerCode;

  @NotEmpty(message = "centralServerAddress is required")
  private String centralServerAddress;

  @NotNull(message = "loanTypeId is required")
  private UUID loanTypeId;

  @NotNull(message = "localAgencies is required")
  @Valid
  private List<LocalAgencyDTO> localAgencies;

  @NotEmpty(message = "centralServerKey is required")
  private String centralServerKey;

  @NotEmpty(message = "centralServerSecret is required")
  private String centralServerSecret;
  private String localServerKey;
  private String localServerSecret;
}
