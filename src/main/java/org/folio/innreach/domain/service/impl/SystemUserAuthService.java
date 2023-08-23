package org.folio.innreach.domain.service.impl;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.folio.innreach.util.TokenUtils.parseUserTokenFromCookies;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.common.errors.AuthorizationException;
import org.folio.innreach.client.AuthnClient;
import org.folio.innreach.client.PermissionsClient;
import org.folio.innreach.config.props.SystemUserProperties;
import org.folio.innreach.domain.dto.folio.SystemUser;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.dto.folio.UserToken;
import org.folio.innreach.domain.service.UserService;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Log4j2
@RequiredArgsConstructor
@Service
@EnableConfigurationProperties(SystemUserProperties.class)
public class SystemUserAuthService {

  private final PermissionsClient permissionsClient;
  private final AuthnClient authnClient;
  private final UserService userService;
  private final FolioExecutionContextBuilder contextBuilder;
  private final SystemUserProperties folioSystemUserConf;

  public void setupSystemUser() {
    var folioUser = userService.getUserByName(folioSystemUserConf.getUsername());
    var userId = folioUser.map(User::getId)
      .orElse(UUID.randomUUID());

    if (folioUser.isPresent()) {
      log.info("Setting up existing system user");
      addPermissions(userId);
    } else {
      log.info("No system user exist, creating...");

      createFolioUser(userId);
      saveCredentials();
      assignPermissions(userId);
    }
  }

  public UserToken loginSystemUser(SystemUser systemUser) {
    log.info("loginSystemUser:: username : {}, token: {}", systemUser.getUserName(), systemUser.getToken().accessToken());
    try (var context = new FolioExecutionContextSetter(contextBuilder.forSystemUser(systemUser))) {
      AuthnClient.UserCredentials creds = AuthnClient.UserCredentials
        .of(systemUser.getUserName(), folioSystemUserConf.getPassword());
      log.info("loginSystemUser::creds username: {}", systemUser.getUserName());
      var token = getTokenWithExpiry(creds, systemUser.getUserName());
      if (isNull(token)) {
        log.info("loginSystemUser::token is null");
        token  = getTokenLegacy(creds, systemUser.getUserName());
      }
      return token;
    }
  }

  private UserToken getTokenLegacy(AuthnClient.UserCredentials credentials, String userName) {
    log.info("getTokenLegacy:: username : {}", userName);
    var responseOptional =
        ofNullable(authnClient.login(credentials));

    if (responseOptional.isEmpty()) {
      throw new AuthorizationException("Unexpected response from login: " + userName);
    }

    var response = responseOptional.get();
    if (response.getStatusCode() == HttpStatusCode.valueOf(404)) {
      log.info("getTokenLegacy:: response 404");
      return null;
    }

    var accessToken = ofNullable(response.getHeaders()
        .get(XOkapiHeaders.TOKEN))
        .orElseThrow(() -> new AuthorizationException("Cannot retrieve okapi token for tenant: " + userName))
        .get(0);
    log.info("getTokenLegacy:: usertoken generated ");
    return UserToken.builder()
        .accessToken(accessToken)
        .accessTokenExpiration(Instant.MAX)
        .build();
  }

  private UserToken getTokenWithExpiry(AuthnClient.UserCredentials credentials, String userName) {
    var responseOptional =
        ofNullable(authnClient.loginWithExpiry(credentials));

    if (responseOptional.isEmpty()) {
      throw new AuthorizationException("Unexpected response from loginWithExpiry: " + userName);
    }

    var response = responseOptional.get();
    if (response.getStatusCode() == HttpStatusCode.valueOf(404)) {
      return null;
    }

    if (isNull(response) || isNull(response.getBody())) {
      throw new IllegalStateException(String.format(
          "User [%s] cannot %s because expire times missing for status %s",
          userName, "login with expiry", response.getStatusCode()));
    }

    return ofNullable(response.getHeaders().get(SET_COOKIE))
        .filter(list -> !CollectionUtils.isEmpty(list))
        .map(cookieHeaders -> parseUserTokenFromCookies(cookieHeaders, response.getBody()))
        .orElseThrow(() -> new IllegalStateException(String.format(
            "User [%s] cannot %s because of missing tokens", userName, "login with expiry")));
  }

  private void createFolioUser(UUID id) {
    final var user = createUserObject(id);
    userService.saveUser(user);
  }

  private void saveCredentials() {
    authnClient.saveCredentials(AuthnClient.UserCredentials.of(folioSystemUserConf.getUsername(), folioSystemUserConf.getPassword()));

    log.info("Saved credentials for user: [{}]", folioSystemUserConf.getUsername());
  }

  private void assignPermissions(UUID userId) {
    List<String> perms = getResourceLines(folioSystemUserConf.getPermissionsFilePath());

    if (isEmpty(perms)) {
      throw new IllegalStateException("No permissions found to assign to user with id: " + userId);
    }

    var permissions = PermissionsClient.Permissions.of(UUID.randomUUID()
      .toString(), userId, perms);

    permissionsClient.assignPermissionsToUser(permissions);
  }

  private void addPermissions(UUID userId) {
    var expectedPermissions = getResourceLines(folioSystemUserConf.getPermissionsFilePath());
    var assignedPermissions = permissionsClient.getUserPermissions(userId);

    if (isEmpty(expectedPermissions)) {
      throw new IllegalStateException("No permissions found to assign to user with id: " + userId);
    }

    var permissionsToAdd = new HashSet<>(expectedPermissions);
    assignedPermissions.getResult().forEach(permissionsToAdd::remove);

    permissionsToAdd.forEach(permission ->
      permissionsClient.addPermission(userId, PermissionsClient.Permission.of(permission)));
  }

  private User createUserObject(UUID id) {
    final var user = new User();

    user.setId(id);
    user.setActive(true);
    user.setUsername(folioSystemUserConf.getUsername());

    user.setPersonal(new User.Personal());
    user.getPersonal()
      .setLastName(folioSystemUserConf.getLastname());

    return user;
  }

  @SneakyThrows
  private static List<String> getResourceLines(String permissionsFilePath) {
    var resource = new ClassPathResource(permissionsFilePath);
    return IOUtils.readLines(resource.getInputStream(), StandardCharsets.UTF_8);
  }

}
