package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.spring.FolioExecutionContext;

class FolioTenantServiceTest {

  @Mock
  private SystemUserService systemUserService;

  @Mock
  private FolioExecutionContext context;

  @Mock
  private ContributionJobRunner contributionJobRunner;

  @InjectMocks
  private FolioTenantService service;

  @BeforeEach
  void setupBeforeEach() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void shouldInitializeTenant() {
    service.initializeTenant();

    verify(systemUserService).prepareSystemUser();
    verify(contributionJobRunner).restart(any());
  }

  @Test
  void shouldInitializeTenantIfSystemUserInitFailed() {
    doThrow(new RuntimeException("test")).when(systemUserService).prepareSystemUser();

    assertThrows(RuntimeException.class, () -> service.initializeTenant());
  }

}
