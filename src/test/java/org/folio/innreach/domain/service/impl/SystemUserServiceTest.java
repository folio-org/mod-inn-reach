package org.folio.innreach.domain.service.impl;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import static org.folio.spring.integration.XOkapiHeaders.TENANT;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import com.github.benmanes.caffeine.cache.Cache;
import org.assertj.core.api.Assertions;
import org.folio.innreach.domain.dto.folio.UserToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import org.folio.innreach.config.props.SystemUserProperties;
import org.folio.innreach.domain.dto.folio.SystemUser;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.service.UserService;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;

@Import(SystemUserServiceTest.TestContextConfiguration.class)
@SpringBootTest(classes = SystemUserService.class, webEnvironment = NONE)
class SystemUserServiceTest {

  private static final String TENANT_ID = "tenant1";
  private static final String AUTH_TOKEN = "aa.bb.cc";
  private static final String USERNAME = "mod-innreach";
  private static final String CACHE_NAME = "system-user-cache";
  private static final Instant TOKEN_EXPIRATION = Instant.now().plus(1, ChronoUnit.DAYS);
  private static final Instant TOKEN_EXPIRATION1 = Instant.now().minus(1, ChronoUnit.SECONDS);
  private static final String MOCK_TOKEN = "test_token";


  @Autowired
  private SystemUserService systemUserService;

  @Mock
  private Cache<String, SystemUser> userCache;

  @MockBean
  private SystemUserAuthService authService;
  @MockBean
  private UserService userService;
  @MockBean
  private FolioExecutionContextBuilder contextBuilder;

  private SystemUser systemUserValue() {
    SystemUser user = new SystemUser();
    user.setUserName("username");
    user.setOkapiUrl("http://okapi");
    user.setTenantId(TENANT_ID);
    return user;
  }

  @BeforeEach
  void setupBeforeEach() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void shouldPrepareSystemUser() {
    systemUserService.prepareSystemUser();
    verify(authService).setupSystemUser();
  }

  @Test
  void shouldGetAndCacheSystemUserPositive() {
    var expectedUserToken = new UserToken(MOCK_TOKEN, TOKEN_EXPIRATION);
    systemUserService.setSystemUserCache(userCache);
    when(authService.loginSystemUser(any(SystemUser.class))).thenReturn(expectedUserToken);
    when(contextBuilder.forSystemUser(any(SystemUser.class))).thenReturn(new FolioExecutionContext() {});


    var user = new User();
    user.setUsername(USERNAME);
    user.setId(UUID.randomUUID());
    when(userService.getUserByName(USERNAME)).thenReturn(Optional.of(user));

    SystemUser userTmp = new SystemUser();
    userTmp.setUserId(user.getId());
    userTmp.setUserName(USERNAME);
    userTmp.setOkapiUrl("http://okapi");
    userTmp.setToken(expectedUserToken);
    userTmp.setTenantId(TENANT_ID);
    when(userCache.get(eq(TENANT_ID), any())).thenReturn(userTmp);

    var systemUser = systemUserService.getAuthedSystemUser(TENANT_ID);

    Assertions.assertThat(systemUser).isNotNull();
    Assertions.assertThat(systemUser.getToken()).isNotNull();
    assertThat(systemUser.getTenantId(), is(TENANT_ID));
    assertThat(systemUser.getToken(), is(expectedUserToken));
    assertThat(systemUser.getUserName(), is(USERNAME));
    assertThat(systemUser.getUserId(), is(user.getId()));
    verify(userCache).get(eq(TENANT_ID), any());
  }

  @Test
  void shouldGetAndCacheSystemUserNegative() {
    var expectedUserToken = new UserToken(MOCK_TOKEN, TOKEN_EXPIRATION);
    when(authService.loginSystemUser(any(SystemUser.class))).thenReturn(expectedUserToken);
    when(contextBuilder.forSystemUser(any(SystemUser.class))).thenReturn(new FolioExecutionContext() {});


    var user = new User();
    user.setUsername(USERNAME);
    user.setId(UUID.randomUUID());
    when(userService.getUserByName(USERNAME)).thenReturn(Optional.of(user));

    SystemUser userTmp = new SystemUser();
    userTmp.setUserId(user.getId());
    userTmp.setUserName(USERNAME);
    userTmp.setOkapiUrl("http://okapi");
    userTmp.setToken(expectedUserToken);
    userTmp.setTenantId(TENANT_ID);
    when(userCache.get(eq(TENANT_ID), any())).thenReturn(null);

    var systemUser = systemUserService.getAuthedSystemUser(TENANT_ID);

    Assertions.assertThat(systemUser).isNotNull();
    Assertions.assertThat(systemUser.getToken()).isNotNull();
    assertThat(systemUser.getTenantId(), is(TENANT_ID));
    assertThat(systemUser.getToken(), is(expectedUserToken));
    assertThat(systemUser.getUserName(), is(USERNAME));
    assertThat(systemUser.getUserId(), is(user.getId()));
  }

  @Test
  void shouldGetAndSystemUserTokenExpiry() {
    var expectedUserToken = new UserToken(MOCK_TOKEN, TOKEN_EXPIRATION1);
    systemUserService.setSystemUserCache(userCache);
    when(authService.loginSystemUser(any(SystemUser.class))).thenReturn(expectedUserToken);
    when(contextBuilder.forSystemUser(any(SystemUser.class))).thenReturn(new FolioExecutionContext() {});


    var user = new User();
    user.setUsername(USERNAME);
    user.setId(UUID.randomUUID());
    when(userService.getUserByName(USERNAME)).thenReturn(Optional.of(user));

    SystemUser userTmp = new SystemUser();
    userTmp.setUserId(user.getId());
    userTmp.setUserName(USERNAME);
    userTmp.setOkapiUrl("http://okapi");
    userTmp.setToken(expectedUserToken);
    userTmp.setTenantId(TENANT_ID);
    when(userCache.get(eq(TENANT_ID), any())).thenReturn(userTmp);

    var systemUser = systemUserService.getAuthedSystemUser(TENANT_ID);

    Assertions.assertThat(systemUser).isNotNull();
    Assertions.assertThat(systemUser.getToken()).isNotNull();
    assertThat(systemUser.getTenantId(), is(TENANT_ID));
    assertThat(systemUser.getToken(), is(expectedUserToken));
    assertThat(systemUser.getUserName(), is(USERNAME));
    assertThat(systemUser.getUserId(), is(user.getId()));
    verify(userCache).get(eq(TENANT_ID), any());
  }


  @TestConfiguration
  @TestPropertySource("classpath:application.yml")
  @EnableConfigurationProperties(SystemUserProperties.class)
  static class TestContextConfiguration {

    @Bean
    FolioExecutionContext folioExecutionContext() {
      return new DefaultFolioExecutionContext(null, singletonMap(TENANT, singletonList(TENANT_ID)));
    }
  }

}
