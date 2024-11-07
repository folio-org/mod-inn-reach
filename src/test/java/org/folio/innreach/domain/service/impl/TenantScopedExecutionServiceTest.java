package org.folio.innreach.domain.service.impl;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.context.ExecutionContextBuilder;
import org.folio.spring.model.SystemUser;
import org.folio.spring.service.SystemUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantScopedExecutionServiceTest {
  @Mock
  private ExecutionContextBuilder contextBuilder;
  @Mock
  private SystemUserService systemUserService;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @InjectMocks
  private TenantScopedExecutionService tenantScopedExecutionService;

  @Test
  void testRunTenantScoped_withSystemUserService() {
    String tenantId = "testTenant";
    Runnable job = mock(Runnable.class);

    when(systemUserService.getAuthedSystemUser(tenantId)).thenReturn(SystemUser.builder().build());
    when(contextBuilder.forSystemUser(any())).thenReturn(folioExecutionContext);

    tenantScopedExecutionService.setSystemUserService(systemUserService);

    tenantScopedExecutionService.executeAsyncTenantScoped(tenantId, job);

    verify(systemUserService).getAuthedSystemUser(tenantId);
    verify(contextBuilder).forSystemUser(any());
    verify(job).run();
  }

  @Test
  void testRunTenantScoped_withoutSystemUserService() {
    String tenantId = "testTenant";
    Runnable job = mock(Runnable.class);

    when(contextBuilder.buildContext(any())).thenReturn(folioExecutionContext);

    tenantScopedExecutionService.executeAsyncTenantScoped(tenantId, job);

    verify(systemUserService, never()).getAuthedSystemUser(tenantId);
    verify(contextBuilder).buildContext(any());
    verify(job).run();
  }

}
