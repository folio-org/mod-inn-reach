package org.folio.innreach.domain.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.model.SystemUser;
import org.folio.spring.model.UserToken;
import org.junit.jupiter.api.Test;

import org.folio.spring.FolioModuleMetadata;


class FolioExecutionContextBuilderTest {

  private final FolioExecutionContext folioExecutionContext = mock(FolioExecutionContext.class);
  private final FolioModuleMetadata folioModuleMetadata = mock(FolioModuleMetadata.class);
  private final InnReachFolioExecutionContextBuilder builder =
    new InnReachFolioExecutionContextBuilder(mock(FolioModuleMetadata.class));


  @Test
  void canCreateDbOnlyContext() {
    var context = builder.dbOnlyContext("tenant");

    assertThat(context.getTenantId()).isEqualTo("tenant");
    assertThat(context.getFolioModuleMetadata()).isNotNull();
  }

  @Test
  void canCreateSystemUserContext() {
    UUID userId = UUID.randomUUID();

    var systemUser = SystemUser.builder()
      .userId(userId.toString())
      .okapiUrl("okapi")
      .username("username")
      .tenantId("tenant")
      .token(new UserToken("token", Instant.MAX)).build();

    var context = builder.forSystemUser(systemUser);

    assertThat(context.getTenantId()).isEqualTo("tenant");
    assertThat(context.getToken()).isEqualTo("token");
    assertThat(context.getUserId()).isEqualTo(userId);
    assertThat(context.getOkapiUrl()).isEqualTo("okapi");

    assertThat(context.getAllHeaders()).isNotNull();
    assertThat(context.getOkapiHeaders()).isNotNull();
    assertThat(context.getFolioModuleMetadata()).isNotNull();

    systemUser = SystemUser.builder()
      .userId(userId.toString())
      .okapiUrl("okapi")
      .username("username")
      .tenantId("tenant")
      .token(null).build();

    context = builder.forSystemUser(systemUser);

    assertThat(context.getTenantId()).isEqualTo("tenant");
    assertThat(context.getToken()).isNull();
    assertThat(context.getUserId()).isEqualTo(userId);
    assertThat(context.getOkapiUrl()).isEqualTo("okapi");

    assertThat(context.getAllHeaders()).isNotNull();
    assertThat(context.getOkapiHeaders()).isNotNull();
    assertThat(context.getFolioModuleMetadata()).isNotNull();
  }

  @Test
  void canCreateContextWithUserID(){
    when(folioExecutionContext.getTenantId()).thenReturn("tenantId");
    when(folioExecutionContext.getToken()).thenReturn("token");
    when(folioExecutionContext.getFolioModuleMetadata()).thenReturn(folioModuleMetadata);
    when(folioExecutionContext.getRequestId()).thenReturn("requestId");
    when(folioExecutionContext.getOkapiUrl()).thenReturn("okapiUrl");
    var userId = UUID.randomUUID();

    var context = builder.withUserId(folioExecutionContext, userId);
    assertThat(context.getUserId()).isEqualTo(userId);
    assertThat(context.getFolioModuleMetadata()).isEqualTo(folioModuleMetadata);
    assertThat(context.getToken()).isEqualTo("token");
    assertThat(context.getRequestId()).isEqualTo("requestId");
    assertThat(context.getOkapiUrl()).isEqualTo("okapiUrl");
    assertThat(context.getTenantId()).isEqualTo("tenantId");

    context = builder.withUserId(folioExecutionContext, null);
    assertThat(context.getUserId()).isNull();
    assertThat(context.getFolioModuleMetadata()).isEqualTo(folioModuleMetadata);
    assertThat(context.getToken()).isEqualTo("token");
    assertThat(context.getRequestId()).isEqualTo("requestId");
    assertThat(context.getOkapiUrl()).isEqualTo("okapiUrl");
    assertThat(context.getTenantId()).isEqualTo("tenantId");
  }

}
