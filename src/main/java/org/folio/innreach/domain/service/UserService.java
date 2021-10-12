package org.folio.innreach.domain.service;

import java.util.Optional;
import java.util.UUID;

import org.folio.innreach.domain.dto.folio.User;

public interface UserService {

  Optional<User> getUserById(UUID id);

  Optional<User> getUserByName(String name);

  Optional<User> getUserByPublicId(String userPublicId);

  User saveUser(User user);

}
