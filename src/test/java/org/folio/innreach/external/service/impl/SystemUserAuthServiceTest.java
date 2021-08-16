package org.folio.innreach.external.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.config.props.SystemUserProperties;
import org.folio.innreach.domain.service.impl.FolioExecutionContextBuilder;
import org.folio.innreach.external.client.feign.AuthnClient;
import org.folio.innreach.external.client.feign.PermissionsClient;
import org.folio.innreach.external.client.feign.UsersClient;
import org.folio.innreach.external.dto.ResultList;

@ExtendWith(MockitoExtension.class)
class SystemUserAuthServiceTest {
  @Mock
  private UsersClient usersClient;
  @Mock
  private AuthnClient authnClient;
  @Mock
  private PermissionsClient permissionsClient;
  @Mock
  private FolioExecutionContextBuilder contextBuilder;

  @Test
  void shouldCreateSystemUserWhenNotExist() {
    when(usersClient.query(any())).thenReturn(userNotExistResponse());

    prepareSystemUser(systemUser());

    verify(usersClient).saveUser(any());
    verify(permissionsClient).assignPermissionsToUser(any());
  }

  @Test
  void shouldNotCreateSystemUserWhenExists() {
    when(usersClient.query(any())).thenReturn(userExistsResponse());
    when(permissionsClient.getUserPermissions(any())).thenReturn(ResultList.empty());

    prepareSystemUser(systemUser());

    verify(permissionsClient, times(2)).addPermission(any(), any());
  }

  @Test
  void cannotUpdateUserIfEmptyPermissions() {
    var systemUser = systemUserNoPermissions();
    when(usersClient.query(any())).thenReturn(userNotExistResponse());

    assertThrows(IllegalStateException.class, () -> prepareSystemUser(systemUser));

    verifyNoInteractions(permissionsClient);
  }

  @Test
  void cannotCreateUserIfEmptyPermissions() {
    var systemUser = systemUserNoPermissions();
    when(usersClient.query(any())).thenReturn(userExistsResponse());

    assertThrows(IllegalStateException.class, () -> prepareSystemUser(systemUser));
  }

  @Test
  void shouldAddOnlyNewPermissions() {
    when(usersClient.query(any())).thenReturn(userExistsResponse());
    when(permissionsClient.getUserPermissions(any()))
      .thenReturn(ResultList.asSinglePage("inventory-storage.instance.item.get"));

    prepareSystemUser(systemUser());

    verify(permissionsClient, times(1)).addPermission(any(), any());
    verify(permissionsClient, times(0))
      .addPermission(any(), eq(PermissionsClient.Permission.of("inventory-storage.instance.item.get")));
    verify(permissionsClient, times(1))
      .addPermission(any(), eq(PermissionsClient.Permission.of("inventory-storage.instance.item.post")));
  }

  private SystemUserProperties systemUser() {
    return SystemUserProperties.builder()
      .password("password")
      .username("username")
      .permissionsFilePath("permissions/test-permissions.csv")
      .build();
  }

  private SystemUserProperties systemUserNoPermissions() {
    return SystemUserProperties.builder()
      .password("password")
      .username("username")
      .permissionsFilePath("permissions/empty-permissions.csv")
      .build();
  }

  private ResultList<UsersClient.User> userExistsResponse() {
    return ResultList.asSinglePage(new UsersClient.User());
  }

  private ResultList<UsersClient.User> userNotExistResponse() {
    return ResultList.empty();
  }

  private SystemUserAuthService systemUserService(SystemUserProperties properties) {
    return new SystemUserAuthService(permissionsClient,
      usersClient, authnClient, contextBuilder, properties);
  }

  private void prepareSystemUser(SystemUserProperties properties) {
    systemUserService(properties).setupSystemUser();
  }
}
