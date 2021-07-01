package org.folio.innreach.external.service;

import feign.FeignException.FeignClientException;
import feign.RetryableException;
import org.folio.innreach.external.client.feign.InnReachFeignClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.external.dto.AccessTokenRequestDTO;
import org.folio.innreach.external.exception.InnReachException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class InnReachExternalServiceImplTest {

  @Mock
  private InnReachFeignClient innReachClient;

  @InjectMocks
  private InnReachExternalServiceImpl innReachExternalService;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void returnAccessToken_when_InnReachApiIsUp_and_keySecretAreValid() {
    when(innReachClient.getAccessToken(any(), any())).thenReturn(
      ResponseEntity.ok(new AccessTokenDTO("accessToken", "Bearer", 599)));

    var accessToken = innReachExternalService.getAccessToken(
      new AccessTokenRequestDTO("uri", "key", "secret"));

    assertNotNull(accessToken);
  }

  @Test
  public void throwException_when_keySecretAreNotValid() {
    when(innReachClient.getAccessToken(any(), any())).thenThrow(FeignClientException.class);

    assertThrows(InnReachException.class, () -> innReachExternalService.getAccessToken(
      new AccessTokenRequestDTO("uri", "key", "secret")));
  }

  @Test
  public void throwException_when_keyCentralServerIsNotAccessible() {
    when(innReachClient.getAccessToken(any(), any())).thenThrow(RetryableException.class);

    assertThrows(InnReachException.class, () -> innReachExternalService.getAccessToken(
      new AccessTokenRequestDTO("uri", "key", "secret")));
  }

  @Test
  public void throwException_when_centralServerUriIsNotAbsolute() {
    when(innReachClient.getAccessToken(any(), any())).thenThrow(IllegalArgumentException.class);

    assertThrows(InnReachException.class, () -> innReachExternalService.getAccessToken(
      new AccessTokenRequestDTO("uri", "key", "secret")));
  }
}
