package org.folio.innreach.external.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

@Getter
@Builder
public class SystemUser {
  private final String username;
  @With
  private final String token;
  private final String okapiUrl;
  private final String tenantId;
}
