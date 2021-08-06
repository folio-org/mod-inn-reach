package org.folio.innreach.domain.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class FolioTenantServiceTest {

  @Mock
  private SystemUserService systemUserService;

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
  }

  @Test
  void shouldInitializeTenantIfSystemUserInitFailed() {
    doThrow(new RuntimeException("test")).when(systemUserService).prepareSystemUser();

    assertDoesNotThrow(() -> service.initializeTenant());
  }

}
