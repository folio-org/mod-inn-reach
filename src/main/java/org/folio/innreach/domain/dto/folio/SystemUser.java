package org.folio.innreach.domain.dto.folio;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class SystemUser {
  private final UUID id;
  private final String username;
  private final String token;
  private final String okapiUrl;
  private final String tenantId;
}
