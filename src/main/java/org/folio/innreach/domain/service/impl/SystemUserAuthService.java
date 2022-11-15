package org.folio.innreach.domain.service.impl;

import static org.springframework.util.CollectionUtils.isEmpty;

import static org.folio.innreach.domain.service.impl.FolioExecutionContextUtils.executeWithinContext;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.io.IOUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import org.folio.innreach.client.AuthnClient;
import org.folio.innreach.client.PermissionsClient;
import org.folio.innreach.config.props.SystemUserProperties;
import org.folio.innreach.domain.dto.folio.SystemUser;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.service.UserService;
import org.folio.spring.integration.XOkapiHeaders;

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

  public String loginSystemUser(SystemUser systemUser) {
    try {
      return executeWithinContext(contextBuilder.forSystemUser(systemUser), () -> {

        AuthnClient.UserCredentials creds = AuthnClient.UserCredentials
          .of(systemUser.getUserName(), folioSystemUserConf.getPassword());

        var response = authnClient.getApiKey(creds);

        List<String> tokenHeaders = response.getHeaders().get(XOkapiHeaders.TOKEN);

        return Optional.ofNullable(tokenHeaders)
          .filter(list -> !CollectionUtils.isEmpty(list))
          .map(list -> list.get(0))
          .orElseThrow(() -> new IllegalStateException(String.format("User [%s] cannot log in", systemUser.getUserName())));

      });
    } catch(Exception ex) {
      throw new UnsupportedOperationException(String.format("This operation not permitted for user [%s]", systemUser.getUserName()));
    }
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
