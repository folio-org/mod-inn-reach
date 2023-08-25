package org.folio.innreach.domain.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import feign.FeignException;
import feign.Request;
import feign.Response;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.folio.innreach.domain.dto.folio.SystemUser;
import org.folio.innreach.domain.dto.folio.UserToken;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.client.AuthnClient;
import org.folio.innreach.client.PermissionsClient;
import org.folio.innreach.config.props.SystemUserProperties;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMapAdapter;


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
  public static final String FOLIO_ACCESS_TOKEN = "folioAccessToken";

  @Mock
  private FolioExecutionContext context;
  @Mock
  private SystemUserAuthService systemUserAuthService;
  private static final Instant TOKEN_EXPIRATION = Instant.now().plus(1, ChronoUnit.DAYS);
  private static final String MOCK_TOKEN = "test_token";
  private final ResponseEntity<AuthnClient.LoginResponse> expectedResponse = Mockito.spy(ResponseEntity.of(Optional.of(
    new AuthnClient.LoginResponse(TOKEN_EXPIRATION.toString()))));


  @Test
  void testGetTokenSuccessful() {
    var expectedUserToken = userToken(TOKEN_EXPIRATION);
    var expectedHeaders = cookieHeaders(expectedUserToken.accessToken(), expectedUserToken.accessToken());

    when(authnClient.loginWithExpiry(AuthnClient.UserCredentials.of("username", "password"))).thenReturn(expectedResponse);
    when(contextBuilder.forSystemUser(any())).thenReturn(context);
    when(expectedResponse.getHeaders()).thenReturn(expectedHeaders);
    var systemUser = preparSystmUser();
    var actual = systemUserService(systemUserProperties()).loginSystemUser(systemUser);
    assertThat(actual.accessToken()).isEqualTo(expectedUserToken.accessToken());
  }

  @Test
  void loginSystemUser_when_loginExpiry_notFound() {
    when(authnClient.loginWithExpiry(AuthnClient.UserCredentials.of("username", "password")))
        .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    when(contextBuilder.forSystemUser(any())).thenReturn(context);
    var systemUser = preparSystmUser();
    var systemUserService = systemUserService(systemUserProperties());
    assertThatThrownBy(() -> systemUserService.loginSystemUser(systemUser)).isInstanceOf(IllegalStateException.class)
      .hasMessage(String.format(
        "User [username] cannot %s because expire times missing for status %s", "login with expiry", HttpStatus.NOT_FOUND));
  }
  @Test
  void loginSystemUser_when_loginExpiry_ThrowsFeignException() {

    when(authnClient.loginWithExpiry(AuthnClient.UserCredentials.of("username", "password")))
      .thenThrow(feignException());
    var expectedUserToken = new UserToken(MOCK_TOKEN, Instant.MAX);
    when(authnClient.login(AuthnClient.UserCredentials.of("username", "password")))
      .thenReturn(buildClientResponse(MOCK_TOKEN));
    var systemUser = preparSystmUser();
    var systemUserService = systemUserService(systemUserProperties());
    when(contextBuilder.forSystemUser(any())).thenReturn(context);
    var actual = systemUserService.loginSystemUser(systemUser);
    assertThat(actual).isEqualTo(expectedUserToken);
  }

  @Test
  void loginSystemUser_negative_emptyBody() {
    when(authnClient.loginWithExpiry(AuthnClient.UserCredentials.of("username", "password")))
        .thenReturn(new ResponseEntity<>(org.springframework.http.HttpStatus.OK));
    when(contextBuilder.forSystemUser(any())).thenReturn(context);
    var systemUser = preparSystmUser();
    var systemUserService = systemUserService(systemUserProperties());
    assertThatThrownBy(() -> systemUserService.loginSystemUser(systemUser)).isInstanceOf(IllegalStateException.class)
        .hasMessage("User [username] cannot login with expiry because expire times missing for status 200 OK");
  }

  private ResponseEntity<AuthnClient.LoginResponse> buildClientResponse(String token) {
    return ResponseEntity.ok()
        .header(XOkapiHeaders.TOKEN, token)
        .body(new AuthnClient.LoginResponse(TOKEN_EXPIRATION.toString()));
  }

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
  private static FeignException feignException() {
    Map<String, Collection<String>> headersError = new HashMap<>();
    byte[] bodyError = new byte[1];
    return FeignException.errorStatus(
      "loginwithExpiry",
      Response.builder()
        .status(404)
        .reason("message error")
        .request(Request.create(
          Request.HttpMethod.POST,
          "/login-with-expiry",
          headersError,
          null,
          null,
          null))
        .body(bodyError)
        .build());
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

  private static SystemUser preparSystmUser() {
    SystemUser systemUser = new SystemUser();
    systemUser.setUserName("username");
    systemUser.setOkapiUrl("http://okapi");
    systemUser.setTenantId("tenant1");
    return  systemUser;
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

  private HttpHeaders cookieHeaders(String accessToken, String refreshToken) {
    return new HttpHeaders(new MultiValueMapAdapter<>(Map.of(HttpHeaders.SET_COOKIE, List.of(
      new DefaultCookie(FOLIO_ACCESS_TOKEN, accessToken).toString()
    ))));
  }
  private UserToken userToken(Instant accessExpiration) {
    return UserToken.builder()
      .accessToken("access-token")
      .accessTokenExpiration(accessExpiration)
      .build();
  }

  private static SystemUserProperties systemUserProperties() {
    return new SystemUserProperties("username", "password", "system", "permissions/test-permissions.csv");
  }
}
