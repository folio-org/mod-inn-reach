package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FolioTenantServiceTest {

  @Mock
  private SystemUserService systemUserService;

  @Mock
  private KafkaService kafkaService;

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
    verify(kafkaService).restartEventListeners();
  }

  @Test
  void shouldInitializeTenantIfSystemUserInitFailed() {
    doThrow(new RuntimeException("test")).when(systemUserService).prepareSystemUser();

    assertThrows(RuntimeException.class, () -> service.initializeTenant());
  }

}
