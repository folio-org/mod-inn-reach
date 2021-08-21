package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.client.AuthnClient;
import org.folio.innreach.client.PermissionsClient;
import org.folio.innreach.config.props.SystemUserProperties;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.service.UserService;

@ExtendWith(MockitoExtension.class)
class SystemUserAuthServiceTest {
  @Mock
  private UserService userService;
  @Mock
  private AuthnClient authnClient;
  @Mock
  private PermissionsClient permissionsClient;
  @Mock
  private FolioExecutionContextBuilder contextBuilder;

  @Test
  void shouldCreateSystemUserWhenNotExist() {
    when(userService.getUserByName(any())).thenReturn(userNotExistResponse());

    prepareSystemUser(systemUser());

    verify(userService).getUserByName(any());
    verify(permissionsClient).assignPermissionsToUser(any());
  }

  @Test
  void shouldNotCreateSystemUserWhenExists() {
    when(userService.getUserByName(any())).thenReturn(userExistsResponse());
    when(permissionsClient.getUserPermissions(any())).thenReturn(ResultList.empty());

    prepareSystemUser(systemUser());

    verify(permissionsClient, times(2)).addPermission(any(), any());
  }

  @Test
  void cannotUpdateUserIfEmptyPermissions() {
    var systemUser = systemUserNoPermissions();
    when(userService.getUserByName(any())).thenReturn(userNotExistResponse());

    assertThrows(IllegalStateException.class, () -> prepareSystemUser(systemUser));

    verifyNoInteractions(permissionsClient);
  }

  @Test
  void cannotCreateUserIfEmptyPermissions() {
    var systemUser = systemUserNoPermissions();
    when(userService.getUserByName(any())).thenReturn(userExistsResponse());

    assertThrows(IllegalStateException.class, () -> prepareSystemUser(systemUser));
  }

  @Test
  void shouldAddOnlyNewPermissions() {
    when(userService.getUserByName(any())).thenReturn(userExistsResponse());
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

  private Optional<User> userExistsResponse() {
    return Optional.of(new User());
  }

  private Optional<User> userNotExistResponse() {
    return Optional.empty();
  }

  private SystemUserAuthService systemUserService(SystemUserProperties properties) {
    return new SystemUserAuthService(permissionsClient, authnClient, userService, contextBuilder, properties);
  }

  private void prepareSystemUser(SystemUserProperties properties) {
    systemUserService(properties).setupSystemUser();
  }
}
