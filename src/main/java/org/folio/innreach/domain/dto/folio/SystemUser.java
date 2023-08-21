package org.folio.innreach.domain.dto.folio;

import java.util.UUID;

import lombok.Data;

@Data
public class SystemUser {
  private UUID userId;
  private String userName;
  private UserToken token;
  private String okapiUrl;
  private String tenantId;
}
