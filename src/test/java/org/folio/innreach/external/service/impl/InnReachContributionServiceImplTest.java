package org.folio.innreach.external.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.external.client.feign.InnReachContributionClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.external.dto.BibContributionRequest;
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
    CentralServerConnectionDetailsDTO connectionConf = createConnectionDetails();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionConf);
    when(innReachAuthExternalService.getAccessToken(any()))
      .thenReturn(createAccessToken());

    service.contributeBib(UUID.randomUUID(), "test", new BibContributionRequest());

    verify(contributionClient).contributeBib(any(), any(), any(), any(), any(), any());
  }

  @Test
  void lookUpBib() {
    var connDetails = createConnectionDetails();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connDetails);
    when(innReachAuthExternalService.getAccessToken(any()))
      .thenReturn(createAccessToken());

    service.lookUpBib(UUID.randomUUID(), "test");

    verify(contributionClient).lookUpBib(any(), any(), any(), any(), any(), any());
  }

  private AccessTokenDTO createAccessToken() {
    return new AccessTokenDTO("token", "test", 111);
  }

  private CentralServerConnectionDetailsDTO createConnectionDetails() {
    var connDetails = new CentralServerConnectionDetailsDTO();
    connDetails.setConnectionUrl("http://127.0.0.1");
    return connDetails;
  }
}
