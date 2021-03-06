package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.FolioExecutionContextUtils.executeWithinContext;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import org.folio.innreach.config.props.SystemUserProperties;
import org.folio.innreach.domain.dto.folio.SystemUser;
import org.folio.innreach.domain.service.UserService;

@Log4j2
@Service
@RequiredArgsConstructor
public class SystemUserService {

  private final SystemUserAuthService authService;
  private final UserService userService;
  private final SystemUserProperties systemUserConf;
  private final FolioExecutionContextBuilder contextBuilder;

  @Value("${okapi.url}")
  private String okapiUrl;

  public void prepareSystemUser() {
    log.info("Preparing system user...");

    authService.setupSystemUser();

    log.info("System user has been prepared");
  }

  @Cacheable(cacheNames = "system-user-cache", sync = true)
  public SystemUser getSystemUser(String tenantId) {
    log.info("Attempting to issue token for system user [tenantId={}]", tenantId);

    var systemUser = new SystemUser();
    systemUser.setTenantId(tenantId);
    systemUser.setUserName(systemUserConf.getUsername());
    systemUser.setOkapiUrl(okapiUrl);

    var token = authService.loginSystemUser(systemUser);
    log.info("Token for system user has been issued [tenantId={}]", tenantId);
    systemUser.setToken(token);

    var userId = getSystemUserId(systemUser);
    systemUser.setUserId(userId);

    return systemUser;
  }

  private UUID getSystemUserId(SystemUser systemUser) {
    return executeWithinContext(contextBuilder.forSystemUser(systemUser), () ->
      userService.getUserByName(systemUser.getUserName())
        .orElseThrow(() -> new IllegalArgumentException("System user is not found: name = " + systemUser.getUserName()))
        .getId()
    );
  }

}
