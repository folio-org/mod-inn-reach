package org.folio.innreach.external.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.AccessTokenFixture.createAccessToken;
import static org.folio.innreach.fixture.CentralServerFixture.createCentralServer;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.external.client.feign.InnReachClient;
import org.folio.innreach.external.service.InnReachAuthService;
import org.folio.innreach.repository.CentralServerRepository;

class InnReachExternalServiceImplTest {

  @Mock
  private CentralServerRepository centralServerRepository;

  @Mock
  private InnReachAuthService innReachAuthService;

  @Mock
  private InnReachClient innReachClient;

  @InjectMocks
  private InnReachExternalServiceImpl innReachExternalService;

  @BeforeEach
  void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void throwException_when_requestedCentralServerByIdNotFound() {
    when(centralServerRepository.fetchOneWithCredentials(any())).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> innReachExternalService.callInnReachApi(any(),
        "/contribution/itemtypes"));
  }

  @Test
  void callInnReachApi_when_requestedCentralServerDataExists() {
    when(centralServerRepository.fetchOneWithCredentials(any())).thenReturn(Optional.of(createCentralServer()));
    when(innReachAuthService.getAccessToken(any())).thenReturn(createAccessToken());
    when(innReachClient.callInnReachApi(any(), any(), any(), any())).thenReturn("response");

    var innReachResponse = innReachExternalService.callInnReachApi(any(), "/contribution/itemtypes");

    assertNotNull(innReachResponse);

    verify(innReachAuthService).getAccessToken(any());
    verify(innReachClient).callInnReachApi(any(), any(), any(), any());
  }
}
