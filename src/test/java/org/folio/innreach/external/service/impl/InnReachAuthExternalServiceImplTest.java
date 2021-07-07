package org.folio.innreach.external.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.AccessTokenFixture.createAccessToken;
import static org.folio.innreach.fixture.CentralServerFixture.createCentralServerConnectionDetailsDTO;

import com.google.common.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import org.folio.innreach.external.client.feign.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;

class InnReachAuthExternalServiceImplTest {

  @Mock
  private InnReachAuthClient innReachAuthClient;

  @Mock
  private Cache<String, AccessTokenDTO> accessTokenCache;

  @InjectMocks
  private InnReachAuthExternalServiceImpl innReachAuthService;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void returnAccessTokenFromCache_when_tokenIsCached() {
    when(accessTokenCache.getIfPresent(anyString())).thenReturn(createAccessToken());

    var accessToken = innReachAuthService.getAccessToken(createCentralServerConnectionDetailsDTO());

    assertNotNull(accessToken);

    verify(accessTokenCache).getIfPresent(anyString());
    verifyNoInteractions(innReachAuthClient);
  }

  @Test
  void getNewTokenFromInnReach_cacheItAndReturn_when_cacheIsEmpty() {
    when(accessTokenCache.getIfPresent(anyString())).thenReturn(null);
    when(innReachAuthClient.getAccessToken(any(), any())).thenReturn(ResponseEntity.ok(createAccessToken()));

    var accessToken = innReachAuthService.getAccessToken(createCentralServerConnectionDetailsDTO());

    assertNotNull(accessToken);

    verify(innReachAuthClient).getAccessToken(any(), any());
    verify(accessTokenCache).put(anyString(), any(AccessTokenDTO.class));
  }
}
