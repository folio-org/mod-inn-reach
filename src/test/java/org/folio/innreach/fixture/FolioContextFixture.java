package org.folio.innreach.fixture;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import static org.folio.spring.integration.XOkapiHeaders.TENANT;

import org.mockito.Mockito;

import org.folio.innreach.domain.service.impl.InnReachFolioExecutionContextBuilder;
import org.folio.spring.service.SystemUserService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.spring.DefaultFolioExecutionContext;

public class FolioContextFixture {

  public static final DefaultFolioExecutionContext FOLIO_CONTEXT =
    new DefaultFolioExecutionContext(null, singletonMap(TENANT, singletonList("test")));

  private static final InnReachFolioExecutionContextBuilder contextBuilder = Mockito.mock(InnReachFolioExecutionContextBuilder.class);
  private static final SystemUserService systemUserService = Mockito.mock(SystemUserService.class);

  public static TenantScopedExecutionService createTenantExecutionService() {
    return new TenantScopedExecutionService(Mockito.mock(InnReachFolioExecutionContextBuilder.class), Mockito.mock(SystemUserService.class));
  }

}
