package org.folio.innreach.domain.service.impl;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import static org.folio.spring.integration.XOkapiHeaders.TENANT;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.folio.innreach.external.dto.SystemUser;
import org.folio.innreach.external.service.impl.SystemUserAuthService;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;

@Import(SystemUserServiceTest.TestContextConfiguration.class)
@SpringBootTest(classes = SystemUserService.class, webEnvironment = NONE)
class SystemUserServiceTest {

  private static final String TENANT_ID = "tenant1";
  private static final String AUTH_TOKEN = "aa.bb.cc";
  private static final String USERNAME = "mod-innreach";
  private static final String CACHE_NAME = "system-user-cache";

  @Autowired
  private SystemUserService systemUserService;

  @Autowired
  private CacheManager cacheManager;

  @MockBean
  private SystemUserAuthService authService;


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
  void shouldGetAndCacheSystemUser() {
    when(authService.loginSystemUser(any(SystemUser.class))).thenReturn(AUTH_TOKEN);

    var systemUser = systemUserService.getSystemUser(TENANT_ID);

    Assertions.assertThat(systemUser).isNotNull();
    Assertions.assertThat(systemUser.getToken()).isNotNull();
    assertThat(systemUser.getTenantId(), is(TENANT_ID));
    assertThat(systemUser.getToken(), is(AUTH_TOKEN));
    assertThat(systemUser.getUsername(), is(USERNAME));

    Assertions.assertThat(cacheManager.getCache(CACHE_NAME).get(TENANT_ID, SystemUser.class))
      .isEqualTo(systemUser);
  }

  @EnableCaching
  @TestConfiguration
  @TestPropertySource("classpath:application.yml")
  @EnableConfigurationProperties(SystemUserProperties.class)
  static class TestContextConfiguration {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("system-user-cache");
    }

    @Bean
    FolioExecutionContext folioExecutionContext() {
      return new DefaultFolioExecutionContext(null, singletonMap(TENANT, singletonList(TENANT_ID)));
    }
  }

}
