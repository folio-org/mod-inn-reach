package org.folio.innreach.domain.entity.base;

import static java.util.UUID.fromString;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.AuditorAware;

import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.service.UserService;
import org.folio.spring.FolioExecutionContext;

@Log4j2
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<AuditableUser> {

  private final FolioExecutionContext execContext;
  private final UserService userService;


  @Override
  public Optional<AuditableUser> getCurrentAuditor() {
    log.debug("Detecting current auditor by: userId = {}, userName = {}", execContext.getUserId(),
        execContext.getUserName());
    
    var user = userService.getUserById(execContext.getUserId())
        .or(() -> userService.getUserByName(execContext.getUserName()));

    Optional<AuditableUser> auditor = user.map(toAuditor()).or(useSystem());

    log.debug("Auditor detected: {}", auditor.map(AuditableUser::toString).orElse("EMPTY"));

    return auditor;
  }

  private Function<User, AuditableUser> toAuditor() {
    return user -> {
      if (!user.isActive()) {
        throw new IllegalStateException("Current user is not active and cannot be used as an auditor: userId = " +
            user.getId());
      }

      return new AuditableUser(fromString(user.getId()), user.getUsername());
    };
  }

  private Supplier<Optional<? extends AuditableUser>> useSystem() {
    return () -> {
      log.warn("Current auditor cannot be determined from execution context by: userId = {}, userName = {}. " +
          "Using SYSTEM user as auditor", execContext.getUserId(), execContext.getUserName());

      return Optional.of(AuditableUser.SYSTEM);
    };
  }

}