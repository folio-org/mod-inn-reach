package org.folio.innreach.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CentralServerConnectionDetailsDTO {

  private UUID id;
  private String connectionUrl;
  private String localCode;
  private String centralCode;
  private String key;
  private String secret;
}
