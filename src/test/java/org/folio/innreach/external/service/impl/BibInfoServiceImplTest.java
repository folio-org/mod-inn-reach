package org.folio.innreach.external.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.folio.innreach.external.dto.InnReachResponse.ERROR_STATUS;
import static org.folio.innreach.external.dto.InnReachResponse.OK_STATUS;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.InstanceTransformationService;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.domain.service.impl.BibInfoServiceImpl;
import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.external.mapper.InnReachResponseMapper;
import org.folio.innreach.external.mapper.InnReachResponseMapperImpl;

@ExtendWith(MockitoExtension.class)
class BibInfoServiceImplTest {

  @Mock
  private CentralServerService centralServerService;
  @Mock
  private InventoryViewService instanceService;
  @Mock
  private InstanceTransformationService instanceTransformationService;
  @Spy
  private InnReachResponseMapper mapper = new InnReachResponseMapperImpl();

  @InjectMocks
  private BibInfoServiceImpl service;

  @Test
  void shouldReturnBibInfo() {
    var centralServer = new CentralServerDTO();
    centralServer.setId(UUID.randomUUID());

    when(centralServerService.getCentralServerByCentralCode(any(String.class))).thenReturn(centralServer);
    when(instanceService.getInstanceByHrid(any(String.class))).thenReturn(new Instance());
    when(instanceTransformationService.getBibInfo(any(UUID.class), any(Instance.class))).thenReturn(new BibInfo());

    var response = service.getBibInfo("bib001313", "code1");

    assertNotNull(response);
    assertEquals(OK_STATUS, response.getStatus());
  }

  @Test
  void shouldReturnErrorResponse() {
    var centralServer = new CentralServerDTO();
    centralServer.setId(UUID.randomUUID());

    when(centralServerService.getCentralServerByCentralCode(any(String.class))).thenThrow(new RuntimeException());

    var response = service.getBibInfo("bib001313", "code1");

    assertNotNull(response);
    assertEquals(ERROR_STATUS, response.getStatus());
  }
}
