package org.folio.innreach.domain.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import org.folio.innreach.external.dto.SystemUser;
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
    var systemUser = SystemUser.builder()
      .token("token").username("username")
      .okapiUrl("okapi").tenantId("tenant")
      .build();
    var context = builder.forSystemUser(systemUser);

    assertThat(context.getTenantId()).isEqualTo("tenant");
    assertThat(context.getToken()).isEqualTo("token");
    assertThat(context.getUserName()).isEqualTo("username");
    assertThat(context.getOkapiUrl()).isEqualTo("okapi");

    assertThat(context.getAllHeaders()).isNotNull();
    assertThat(context.getOkapiHeaders()).isNotNull();
    assertThat(context.getFolioModuleMetadata()).isNotNull();
  }
}
