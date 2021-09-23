package org.folio.innreach.domain.service.impl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.domain.dto.folio.ResultList.asSinglePage;
import static org.folio.innreach.fixture.ContributionFixture.createInstanceView;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.client.InventoryViewClient;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

  @Mock
  private InventoryViewClient inventoryViewClient;

  @InjectMocks
  private InventoryServiceImpl service;

  @Test
  void shouldGetInstanceById() {
    when(inventoryViewClient.getInstanceById(any(UUID.class))).thenReturn(asSinglePage(createInstanceView()));

    var instance = service.getInstance(UUID.randomUUID());

    assertNotNull(instance);

    verify(inventoryViewClient).getInstanceById(any(UUID.class));
  }

  @Test
  void getInstanceByHrid() {
    when(inventoryViewClient.getInstanceByHrid(any(String.class))).thenReturn(asSinglePage(createInstanceView()));

    var instance = service.getInstanceByHrid("in00343441");

    assertNotNull(instance);

    verify(inventoryViewClient).getInstanceByHrid(any(String.class));
  }
}
