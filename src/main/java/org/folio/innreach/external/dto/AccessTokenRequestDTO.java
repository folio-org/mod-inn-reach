package org.folio.innreach.external.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AccessTokenRequestDTO {
  private String centralServerUri;
  private String key;
  private String secret;
}
