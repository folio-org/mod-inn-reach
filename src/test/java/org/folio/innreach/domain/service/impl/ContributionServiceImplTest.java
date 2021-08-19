package org.folio.innreach.domain.service.impl;

import org.folio.innreach.domain.dto.folio.inventoryStorage.JobResponse;
import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.repository.ContributionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class ContributionServiceImplTest {
  @Mock
  private ContributionRepository repository;

  @Spy
  private InstanceStorageClient client;

  @InjectMocks
  private ContributionServiceImpl service;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void startInitialContributionProcess(){
    when(repository.save(any())).thenReturn(new Contribution());
    when(client.startInitialContribution(any())).thenReturn(new JobResponse());

    service.startInitialContribution(UUID.randomUUID());

    verify(repository, times(2)).save(any());
    verify(client).startInitialContribution(any());
  }
}
