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
import org.springframework.security.crypto.password.PasswordEncoder;

import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.mapper.CentralServerMapper;
import org.folio.innreach.repository.CentralServerRepository;

class CentralServerServiceImplTest {

  @Mock
  private CentralServerRepository centralServerRepository;

  @Mock
  private InnReachExternalService innReachExternalService;

  @Spy
  private CentralServerMapper centralServerMapper = Mappers.getMapper(CentralServerMapper.class);

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private CentralServerServiceImpl centralServerService;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void createCentralServer_when_centralServerIsNew_and_connectionIsOK() {
    when(innReachExternalService.getAccessToken(any())).thenReturn(
      new AccessTokenDTO("accessToken", "Bearer", 599));

    var centralServerDTO = createCentralServerDTO();

    centralServerService.createCentralServer(centralServerDTO);

    verify(innReachExternalService).getAccessToken(any());
    verify(centralServerRepository).save(any());
  }

  @Test
  void returnAllCentralServersDTOS_when_centralServersExist() {
    when(centralServerRepository.fetchAll()).thenReturn(List.of(createCentralServer()));

    var centralServerDTOS = centralServerService.getAllCentralServers();

    verify(centralServerRepository).fetchAll();

    assertNotNull(centralServerDTOS);
    assertFalse(centralServerDTOS.isEmpty());
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

}
