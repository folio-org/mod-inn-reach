package org.folio.innreach.domain.service.impl;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.config.props.SystemUserProperties;
import org.folio.spring.model.SystemUser;
import org.folio.innreach.domain.service.UserService;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class SystemUserServiceInreach {

  private final SystemUserAuthService authService;
  private final UserService userService;
  private final SystemUserProperties systemUserConf;
  private final FolioExecutionContextBuilder contextBuilder;
  private final org.folio.spring.service.SystemUserService folioSystemUserService;

  @Value("${okapi.url}")
  private String okapiUrl;

  public void prepareSystemUser() {
    log.info("Preparing system user...");

    authService.setupSystemUser();

    log.info("System user has been prepared");
  }

  public SystemUser getSystemUser(String tenantId) {
//    log.info("Attempting to issue token for system user [tenantId={}]", tenantId);
//
//    var systemUser = new SystemUser();
//    systemUser.setTenantId(tenantId);
//    systemUser.setUserName(systemUserConf.getUsername());
//    systemUser.setOkapiUrl(okapiUrl);
//
//    var token = authService.loginSystemUser(systemUser);
//    log.info("Token for system user has been issued [tenantId={}]", tenantId);
//    systemUser.setToken(token);
//
//    var userId = getSystemUserId(systemUser);
//    systemUser.setUserId(userId);
//
//    return systemUser;
    return folioSystemUserService.getAuthedSystemUser(tenantId);
  }

  private UUID getSystemUserId(SystemUser systemUser) {
    try (var context = new FolioExecutionContextSetter(contextBuilder.forSystemUser(systemUser))) {
      return userService.getUserByName(systemUser.username())
        .orElseThrow(() -> new IllegalArgumentException("System user is not found: name = " + systemUser.username()))
        .getId();
    }
  }

}
