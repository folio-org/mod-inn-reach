package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.CentralServerFixture.createCentralServer;
import static org.folio.innreach.fixture.CentralServerFixture.createCentralServerDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.external.service.InnReachAuthExternalService;
import org.folio.innreach.mapper.CentralServerMapper;
import org.folio.innreach.repository.CentralServerRepository;

class CentralServerServiceImplTest {

  @Mock
  private CentralServerRepository centralServerRepository;

  @Mock
  private InnReachAuthExternalService innReachAuthExternalService;

  @Spy
  private CentralServerMapper centralServerMapper = Mappers.getMapper(CentralServerMapper.class);

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private CentralServerServiceImpl centralServerService;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.initMocks(this);

    when(passwordEncoder.encode(any())).thenReturn("qwerty");
  }

  @Test
  void createCentralServer_when_centralServerIsNew_and_connectionIsOK() {
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(
      new AccessTokenDTO("accessToken", "Bearer", 599));

    var centralServerDTO = createCentralServerDTO();

    centralServerService.createCentralServer(centralServerDTO);

    verify(innReachAuthExternalService).getAccessToken(any());
    verify(centralServerRepository).save(any());
  }

  @Test
  void returnAllCentralServersDTOS_when_centralServersExist() {
    when(centralServerRepository.getIds(any())).thenReturn(new PageImpl<>(List.of(UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID())));
    when(centralServerRepository.fetchAllById(any())).thenReturn(List.of(createCentralServer(), createCentralServer(),
        createCentralServer()));

    var centralServerDTOS = centralServerService.getAllCentralServers(1, 1);

    verify(centralServerRepository).fetchAllById(any());

    assertNotNull(centralServerDTOS);
    assertNotNull(centralServerDTOS.getCentralServers());
    assertFalse(centralServerDTOS.getCentralServers().isEmpty());
  }

  @Test
  void returnOneCentralServerDTO_when_centralServerExists() {
    when(centralServerRepository.fetchOne(any())).thenReturn(Optional.of(createCentralServer()));

    var centralServerDTO = centralServerService.getCentralServer(UUID.randomUUID());

    verify(centralServerRepository).fetchOne(any());

    assertNotNull(centralServerDTO);
  }

  @Test
  void throwException_when_centralServerDoesNotExist() {
    when(centralServerRepository.fetchOne(any())).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> centralServerService.getCentralServer(UUID.randomUUID()));

    verify(centralServerRepository).fetchOne(any());
  }

  @Test
  void updateCentralServer_when_CentralServerExists() {
    when(centralServerRepository.fetchOne(any())).thenReturn(Optional.of(createCentralServer()));

    centralServerService.updateCentralServer(any(), createCentralServerDTO());

    verify(centralServerRepository).fetchOne(any());
  }

  @Test
  void throwException_when_updatableCentralServerDoesNotExist() {
    when(centralServerRepository.fetchOne(any())).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class,
      () -> centralServerService.updateCentralServer(UUID.randomUUID(), createCentralServerDTO()));

    verify(centralServerRepository).fetchOne(any());
  }

  @Test
  void deleteCentralServer_when_centralServerExists() {
    when(centralServerRepository.findById(any())).thenReturn(Optional.of(createCentralServer()));

    centralServerService.deleteCentralServer(UUID.randomUUID());

    verify(centralServerRepository).delete(any());
  }

  @Test
  void throwException_when_deletableCentralServerDoesNotExist() {
    when(centralServerRepository.findById(any())).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class,
      () -> centralServerService.deleteCentralServer(UUID.randomUUID()));
  }

  @Test
  void updateCentralServerWithNewLocalServerCredentials_when_CentralServerExists() {
    var centralServer = createCentralServer();
    centralServer.setLocalServerCredentials(null);

    when(centralServerRepository.fetchOne(any())).thenReturn(Optional.of(centralServer));

    var centralServerDTO = centralServerService.updateCentralServer(any(), createCentralServerDTO());

    verify(centralServerRepository).fetchOne(any());

    assertNotNull(centralServerDTO.getLocalServerKey());
    assertNotNull(centralServerDTO.getLocalServerSecret());
  }

}
