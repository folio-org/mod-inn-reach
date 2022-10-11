package org.folio.innreach.domain.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.folio.innreach.domain.dto.folio.SystemUser;
import org.folio.spring.FolioModuleMetadata;

class FolioExecutionContextBuilderTest {

  private final FolioExecutionContextBuilder builder =
    new FolioExecutionContextBuilder(mock(FolioModuleMetadata.class));

  @Test
  void canCreateDbOnlyContext() {
    var context = builder.dbOnlyContext("tenant");

    assertThat(context.getTenantId()).isEqualTo("tenant");
    assertThat(context.getFolioModuleMetadata()).isNotNull();
  }

  @Test
  void canCreateSystemUserContext() {
    UUID userId = UUID.randomUUID();

    var systemUser = new SystemUser();
    systemUser.setToken("token");
    systemUser.setOkapiUrl("okapi");
    systemUser.setUserName("username");
    systemUser.setUserId(userId);
    systemUser.setTenantId("tenant");

    var context = builder.forSystemUser(systemUser);

    assertThat(context.getTenantId()).isEqualTo("tenant");
    assertThat(context.getToken()).isEqualTo("token");
    assertThat(context.getUserId()).isEqualTo(userId);
    assertThat(context.getOkapiUrl()).isEqualTo("okapi");

    assertThat(context.getAllHeaders()).isNotNull();
    assertThat(context.getOkapiHeaders()).isNotNull();
    assertThat(context.getFolioModuleMetadata()).isNotNull();
  }

}
