package org.folio.innreach.external.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.external.dto.InnReachResponse.ERROR_STATUS;
import static org.folio.innreach.fixture.AccessTokenFixture.createAccessToken;
import static org.folio.innreach.fixture.CentralServerFixture.createCentralServerConnectionDetailsDTO;

import java.util.UUID;

import org.folio.innreach.external.dto.InnReachResponse;
import org.folio.innreach.external.exception.InnReachTimeOutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.external.client.InnReachContributionClient;
import org.folio.innreach.external.dto.BibItemsInfo;
import org.folio.innreach.external.service.InnReachAuthExternalService;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(MockitoExtension.class)
class InnReachContributionServiceImplTest {

  private static final UUID CENTRAL_SERVER_ID = UUID.randomUUID();
  private static final String BIB_ID = "in0001";
  private static final String ITEM_ID = "it0001";
  private static final String ERROR_MSG = "test";

  @Mock
  private CentralServerService centralServerService;

  @Mock
  private InnReachContributionClient contributionClient;

  @Mock
  private InnReachAuthExternalService innReachAuthExternalService;

  @Mock
  private InnReachResponse response;

  @InjectMocks
  private InnReachContributionServiceImpl service;

  @Test
  void shouldContributeBib() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());

    service.contributeBib(CENTRAL_SERVER_ID, BIB_ID, new BibInfo());

    verify(contributionClient).contributeBib(any(), any(), any(), any(), any(), any());
  }

  @Test
  void lookUpBib() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());

    service.lookUpBib(CENTRAL_SERVER_ID, BIB_ID);

    verify(contributionClient).lookUpBib(any(), any(), any(), any(), any(), any());
  }

  @Test
  void lookUpBib_errorResponse() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());
    when(contributionClient.lookUpBib(any(), any(), any(), any(), any(), any())).thenThrow(new RuntimeException(ERROR_MSG));

    var response = service.lookUpBib(CENTRAL_SERVER_ID, BIB_ID);

    assertNotNull(response);
    assertEquals(ERROR_STATUS, response.getStatus());
    assertEquals(ERROR_MSG, response.getReason());
  }

  @Test
  void deContributeBib() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());
    when(contributionClient.deContributeBib(any(), any(), any(), any(), any())).thenReturn(response);

    service.deContributeBib(CENTRAL_SERVER_ID, BIB_ID);

    verify(contributionClient).deContributeBib(any(), any(), any(), any(), any());
  }

  @Test
  void contributeBibItems() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());

    service.contributeBibItems(CENTRAL_SERVER_ID, BIB_ID, new BibItemsInfo());

    verify(contributionClient).contributeBibItems(any(), any(), any(), any(), any(), any());
  }

  @Test
  void deContributeBibItem() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());
    when(contributionClient.deContributeBibItem(any(), any(), any(), any(), any())).thenReturn(response);

    service.deContributeBibItem(CENTRAL_SERVER_ID, ITEM_ID);

    verify(contributionClient).deContributeBibItem(any(), any(), any(), any(), any());
  }


  @Test
  void lookUpBibItem() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());

    service.lookUpBibItem(CENTRAL_SERVER_ID, BIB_ID, ITEM_ID);

    verify(contributionClient).lookUpBibItem(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void lookUpBibItem_errorResponse() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());
    when(contributionClient.lookUpBibItem(any(), any(), any(), any(), any(), any(), any())).thenThrow(new RuntimeException(ERROR_MSG));

    var response = service.lookUpBibItem(CENTRAL_SERVER_ID, BIB_ID, ITEM_ID);

    assertNotNull(response);
    assertEquals(ERROR_STATUS, response.getStatus());
    assertEquals(ERROR_MSG, response.getReason());
  }

  @Test
  void contributeBib_shouldThrowTimeOutException_whenResourceAccessException() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();
    var bibInfo = new BibInfo();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());
    when(contributionClient.contributeBib(any(), any(), any(), any(), any(), any()))
      .thenThrow(new ResourceAccessException("timeout", new java.net.SocketTimeoutException("Read timed out")));

    assertThrows(InnReachTimeOutException.class,
      () -> service.contributeBib(CENTRAL_SERVER_ID, BIB_ID, bibInfo));
  }

  @Test
  void deContributeBib_shouldThrowTimeOutException_whenResourceAccessException() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());
    when(contributionClient.deContributeBib(any(), any(), any(), any(), any()))
      .thenThrow(new ResourceAccessException("timeout", new java.net.ConnectException("Connection timed out")));

    assertThrows(InnReachTimeOutException.class,
      () -> service.deContributeBib(CENTRAL_SERVER_ID, BIB_ID));
  }

  @Test
  void deContributeBibItem_shouldThrowTimeOutException_whenResourceAccessException() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());
    when(contributionClient.deContributeBibItem(any(), any(), any(), any(), any()))
      .thenThrow(new ResourceAccessException("timeout", new java.net.SocketTimeoutException("Read timed out")));

    assertThrows(InnReachTimeOutException.class,
      () -> service.deContributeBibItem(CENTRAL_SERVER_ID, ITEM_ID));
  }

  @Test
  void contributeBibItems_shouldThrowTimeOutException_whenResourceAccessException() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();
    var bibItemsInfo = new BibItemsInfo();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());
    when(contributionClient.contributeBibItems(any(), any(), any(), any(), any(), any()))
      .thenThrow(new ResourceAccessException("timeout", new java.net.ConnectException("Connection timed out")));

    assertThrows(InnReachTimeOutException.class,
      () -> service.contributeBibItems(CENTRAL_SERVER_ID, BIB_ID, bibItemsInfo));
  }

  @Test
  void lookUpBib_shouldThrowTimeOutException_whenResourceAccessException() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());
    when(contributionClient.lookUpBib(any(), any(), any(), any(), any(), any()))
      .thenThrow(new ResourceAccessException("timeout", new java.net.SocketTimeoutException("Read timed out")));

    assertThrows(InnReachTimeOutException.class,
      () -> service.lookUpBib(CENTRAL_SERVER_ID, BIB_ID));
  }

  @Test
  void lookUpBibItem_shouldThrowTimeOutException_whenResourceAccessException() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());
    when(contributionClient.lookUpBibItem(any(), any(), any(), any(), any(), any(), any()))
      .thenThrow(new ResourceAccessException("timeout", new java.net.SocketTimeoutException("Read timed out")));

    assertThrows(InnReachTimeOutException.class,
      () -> service.lookUpBibItem(CENTRAL_SERVER_ID, BIB_ID, ITEM_ID));
  }

  @Test
  void contributeBib_shouldThrowTimeOutException_whenResourceAccessExceptionWithNonTimeoutCause() {
    var connectionDetails = createCentralServerConnectionDetailsDTO();
    var bibInfo = new BibInfo();

    when(centralServerService.getCentralServerConnectionDetails(any())).thenReturn(connectionDetails);
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());
    when(contributionClient.contributeBib(any(), any(), any(), any(), any(), any()))
      .thenThrow(new ResourceAccessException("I/O error", new java.io.IOException("Connection reset")));

    assertThrows(InnReachTimeOutException.class,
      () -> service.contributeBib(CENTRAL_SERVER_ID, BIB_ID, bibInfo));
  }

}
