package org.folio.innreach.domain.service.impl;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import static org.folio.spring.integration.XOkapiHeaders.TENANT;

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
import org.folio.innreach.domain.service.UserService;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;

@Import(SystemUserServiceInreachTest.TestContextConfiguration.class)
@SpringBootTest(classes = SystemUserServiceInreach.class, webEnvironment = NONE)
class SystemUserServiceInreachTest {

  private static final String TENANT_ID = "tenant1";
  private static final String AUTH_TOKEN = "aa.bb.cc";
  private static final String USERNAME = "mod-innreach";
  private static final String CACHE_NAME = "system-user-cache";

  @Autowired
  private SystemUserServiceInreach systemUserServiceInreach;

  @Autowired
  private CacheManager cacheManager;

  @MockBean
  private SystemUserAuthService authService;
  @MockBean
  private UserService userService;
  @MockBean
  private FolioExecutionContextBuilder contextBuilder;

  @MockBean
  private org.folio.spring.service.SystemUserService systemUserService1;


  @BeforeEach
  void setupBeforeEach() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void shouldPrepareSystemUser() {
    systemUserServiceInreach.prepareSystemUser();

    verify(authService).setupSystemUser();
  }

//  @Test
//  void shouldGetAndCacheSystemUser() {
////    when(authService.loginSystemUser(any(SystemUser.class))).thenReturn(AUTH_TOKEN);
//
////    when(contextBuilder.forSystemUser(any(SystemUser.class))).thenReturn(new FolioExecutionContext() {});
//
//    var user = new User();
//    user.setUsername(USERNAME);
//    user.setId(UUID.randomUUID());
//    when(userService.getUserByName(USERNAME)).thenReturn(Optional.of(user));
//
//    var systemUser = systemUserService.getSystemUser(TENANT_ID);
//
//    Assertions.assertThat(systemUser).isNotNull();
//    Assertions.assertThat(systemUser.token()).isNotNull();
//    assertThat(systemUser.tenantId(), is(TENANT_ID));
//    assertThat(systemUser.token(), is(AUTH_TOKEN));
//    assertThat(systemUser.username(), is(USERNAME));
//    assertThat(systemUser.userId(), is(user.getId()));
//
//    Assertions.assertThat(cacheManager.getCache(CACHE_NAME).get(TENANT_ID, SystemUser.class))
//      .isEqualTo(systemUser);
//  }

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