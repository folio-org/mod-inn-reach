package org.folio.innreach.domain.service;

import java.util.Optional;
import java.util.UUID;

import org.folio.innreach.client.UsersClient;
import org.folio.innreach.domain.dto.folio.User;

public interface UserService {

  Optional<User> getUserById(UUID id);

  Optional<UsersClient.User> getUserByName(String name);

  Optional<User> getUserByBarcode(String userPublicId);

  User saveUser(User user);

  Optional<User> getUserByQuery(String query);
}
