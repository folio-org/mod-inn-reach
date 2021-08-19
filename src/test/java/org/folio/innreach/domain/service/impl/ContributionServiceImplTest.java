package org.folio.innreach.domain.service.impl;

import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.external.client.feign.InventoryStorageClient;
import org.folio.innreach.repository.ContributionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class ContributionServiceImplTest {
  @Mock
  private ContributionRepository repository;

  @Spy
  private InventoryStorageClient client;

  @InjectMocks
  private ContributionServiceImpl service;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void startInitialContributionProcess(){
    when(repository.save(any())).thenReturn(new Contribution());
    doNothing().when(client).startInitialContribution(any());

    service.startInitialContribution(UUID.randomUUID());

    verify(repository).save(any());
    verify(client).startInitialContribution(any());
  }
}
