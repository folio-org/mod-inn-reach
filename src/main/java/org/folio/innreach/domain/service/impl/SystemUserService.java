package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.config.props.SystemUserProperties;
import org.folio.innreach.external.dto.SystemUser;
import org.folio.innreach.external.service.impl.SystemUserAuthService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class SystemUserService {

  private final SystemUserAuthService authService;
  private final FolioExecutionContext context;
  private final SystemUserProperties systemUserConf;

  public void prepareSystemUser() {
    log.info("Preparing system user...");

    authService.setupSystemUser();

    log.info("System user has been prepared");
  }

  @Cacheable(cacheNames = "system-user-cache", sync = true)
  public SystemUser getSystemUser(String tenantId) {
    log.info("Attempting to issue token for system user [tenantId={}]", tenantId);
    var systemUser = SystemUser.builder()
      .tenantId(tenantId)
      .username(systemUserConf.getUsername())
      .okapiUrl(context.getOkapiUrl())
      .build();

    var token = authService.loginSystemUser(systemUser);

    log.info("Token for system user has been issued [tenantId={}]", tenantId);
    return systemUser.withToken(token);
  }

}
