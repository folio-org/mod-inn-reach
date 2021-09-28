package org.folio.innreach.external.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.AccessTokenFixture.createAccessToken;
import static org.folio.innreach.fixture.CentralServerFixture.createCentralServerConnectionDetailsDTO;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.external.client.feign.InnReachContributionClient;
import org.folio.innreach.external.service.InnReachAuthExternalService;

@ExtendWith(MockitoExtension.class)
class InnReachContributionServiceImplTest {

  @Mock
  private CentralServerService centralServerService;

  @Mock
  private InnReachContributionClient contributionClient;

  @Mock
  private InnReachAuthExternalService innReachAuthExternalService;

  @InjectMocks
  private InnReachContributionServiceImpl service;

  @Test
  void shouldContributeBib() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any()))
      .thenReturn(createAccessToken());

    service.contributeBib(UUID.randomUUID(), "test", new BibInfo());

    verify(contributionClient).contributeBib(any(), any(), any(), any(), any(), any());
  }

  @Test
  void lookUpBib() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any()))
      .thenReturn(createAccessToken());

    service.lookUpBib(UUID.randomUUID(), "test");

    verify(contributionClient).lookUpBib(any(), any(), any(), any(), any(), any());
  }

}
