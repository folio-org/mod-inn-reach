package org.folio.innreach.domain.service.impl;

import java.time.Instant;
import java.util.UUID;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.config.props.SystemUserProperties;
import org.folio.innreach.domain.dto.folio.SystemUser;
import org.folio.innreach.domain.service.UserService;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class SystemUserService {

  private final SystemUserAuthService authService;
  private final UserService userService;
  private final SystemUserProperties systemUserConf;
  private final FolioExecutionContextBuilder contextBuilder;
  private Cache<String, SystemUser> systemUserCache;

  @Value("${okapi.url}")
  private String okapiUrl;

  public void prepareSystemUser() {
    log.info("Preparing system user...");

    authService.setupSystemUser();

    log.info("System user has been prepared");
  }
  @Autowired(required = false)
  public void setSystemUserCache(Cache<String, SystemUser> systemUserCache) {
    this.systemUserCache = systemUserCache;
  }

//  @Cacheable(cacheNames = "system-user-cache", sync = true)
    public SystemUser getAuthedSystemUser(String tenantId) {
      log.info("Attempting to issue token for system user [tenantId={}]", tenantId);
      if (systemUserCache == null) {
        log.info("getAuthedSystemUser:: getSystemUser when cache is null");
        return getSystemUser(tenantId);
      }

      var user = systemUserCache.get(tenantId, this::getSystemUser);
      var userToken = user.getToken();
      var now = Instant.now();
      if (userToken.accessTokenExpiration().isAfter(now)) {
        return user;
      }

      systemUserCache.invalidate(tenantId);
      log.info("getAuthedSystemUser:: getSystemUser when token is exipred");
      user = getSystemUser(tenantId);
      systemUserCache.put(tenantId, user);

      return user;
    }

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
    try (var context = new FolioExecutionContextSetter(contextBuilder.forSystemUser(systemUser))) {
      return userService.getUserByName(systemUser.getUserName())
        .orElseThrow(() -> new IllegalArgumentException("System user is not found: name = " + systemUser.getUserName()))
        .getId();
    }
  }

}
