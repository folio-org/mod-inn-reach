package org.folio.innreach.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class LocalAgencyDTO {
  private UUID id;

  @NotEmpty(message = "LocalAgency code is required")
  private String code;

  @NotNull(message = "LocalAgency folioLibraryIds are required")
  private List<UUID> folioLibraryIds;
}
