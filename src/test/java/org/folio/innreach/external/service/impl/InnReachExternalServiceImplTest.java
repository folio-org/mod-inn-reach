package org.folio.innreach.external.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.AccessTokenFixture.createAccessToken;
import static org.folio.innreach.fixture.CentralServerFixture.createCentralServerConnectionDetailsDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.external.client.feign.InnReachClient;
import org.folio.innreach.external.service.InnReachAuthExternalService;

class InnReachExternalServiceImplTest {

  @Mock
  private CentralServerService centralServerService;

  @Mock
  private InnReachAuthExternalService innReachAuthExternalService;

  @Mock
  private InnReachClient innReachClient;

  @InjectMocks
  private InnReachExternalServiceImpl innReachExternalService;

  @BeforeEach
  void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void callInnReachApi_when_centralServerConnectionDetailsAreCorrect() {
    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(createCentralServerConnectionDetailsDTO());
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());
    when(innReachClient.callInnReachApi(any(), any(), any(), any())).thenReturn("response");

    var innReachResponse = innReachExternalService.callInnReachApi(any(), "/contribution/itemtypes");

    assertNotNull(innReachResponse);

    verify(innReachAuthExternalService).getAccessToken(any());
    verify(innReachClient).callInnReachApi(any(), any(), any(), any());
  }
}
