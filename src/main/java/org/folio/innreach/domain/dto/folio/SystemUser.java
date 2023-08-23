package org.folio.innreach.domain.dto.folio;

import java.util.UUID;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SystemUser {
  private UUID userId;
  private String userName;
  private UserToken token;
  private String okapiUrl;
  private String tenantId;

  public SystemUser(String s, String username, UserToken t, String s1, String s2, String tenantId) {
  }
}
