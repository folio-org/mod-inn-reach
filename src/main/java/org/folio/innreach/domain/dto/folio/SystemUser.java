package org.folio.innreach.domain.dto.folio;

import java.util.UUID;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SystemUser {
  private UUID userId;
  private String userName;
  private String token;
  private String okapiUrl;
  private String tenantId;
}
