package org.folio.innreach.domain.service.impl;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.UsersClient;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.service.UserService;
import org.folio.innreach.util.ListUtils;

@Log4j2
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

  private static final String CACHE_USERS_BY_ID = "users-by-id";
  private static final String CACHE_USERS_BY_NAME = "users-by-name";

  private final UsersClient usersClient;

  @Override
  @Cacheable(cacheNames = CACHE_USERS_BY_ID, key = "@folioExecutionContext.tenantId + ': ' + #id")
  public Optional<User> getUserById(UUID id) {
    log.debug("Getting user by id: userId = {}", id);

    if (id == null) {
      return Optional.empty();
    }

    return usersClient.getUserById(id);
  }

  @Override
  @Cacheable(cacheNames = CACHE_USERS_BY_NAME, key = "@folioExecutionContext.tenantId + ': ' + #name")
  public Optional<User> getUserByName(String name) {
    log.debug("Getting user by name: userName = {}", name);

    if (StringUtils.isEmpty(name)) {
      return Optional.empty();
    }

    var users = usersClient.query("username==" + name);

    return ListUtils.getFirstItem(users);
  }

  @Override
  public Optional<User> getUserByBarcode(String userPublicId) {
    var users = usersClient.query(String.format("barcode==%1$s", userPublicId));

    return ListUtils.getFirstItem(users);
  }

  @Override
  public Optional<User> getUserByQuery(String query) {
    var users = usersClient.query(query);

    return ListUtils.getFirstItem(users);
  }

  @Override
  @Caching(put = {
    @CachePut(cacheNames = CACHE_USERS_BY_ID, key = "@folioExecutionContext.tenantId + ': ' + #result.id"),
    @CachePut(cacheNames = CACHE_USERS_BY_NAME, key = "@folioExecutionContext.tenantId + ': ' + #result.username")
  })
  public User saveUser(User user) {
    usersClient.saveUser(user);
    return user;
  }
}
