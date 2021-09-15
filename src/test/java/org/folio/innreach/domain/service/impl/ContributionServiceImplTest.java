package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.dto.ContributionDTO.StatusEnum.COMPLETE;
import static org.folio.innreach.dto.MappingValidationStatusDTO.VALID;
import static org.folio.innreach.fixture.ContributionFixture.createContribution;
import static org.folio.innreach.fixture.JobResponseFixture.createJobResponse;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.domain.entity.ContributionError;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.dto.ContributionErrorDTO;
import org.folio.innreach.mapper.ContributionMapper;
import org.folio.innreach.mapper.ContributionMapperImpl;
import org.folio.innreach.mapper.MappingMethods;
import org.folio.innreach.repository.ContributionErrorRepository;
import org.folio.innreach.repository.ContributionRepository;

class ContributionServiceImplTest {

  @Mock
  private ContributionRepository repository;

  @Mock
  private ContributionErrorRepository errorRepository;

  @Spy
  private InstanceStorageClient client;

  @Mock
  private ContributionJobRunner jobRunner;

  @Spy
  private ContributionMapper mapper = new ContributionMapperImpl(new MappingMethods());

  @Mock
  private ContributionValidationService validationService;

  @InjectMocks
  private ContributionServiceImpl service;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void startInitialContributionProcess() {
    when(repository.save(any(Contribution.class))).thenReturn(createContribution());
    when(client.startInitialContribution(any())).thenReturn(createJobResponse());
    when(validationService.getItemTypeMappingStatus(any())).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(any())).thenReturn(VALID);

    service.startInitialContribution(UUID.randomUUID());

    verify(repository).save(any(Contribution.class));
    verify(client).startInitialContribution(any());
    verify(jobRunner).run(any(UUID.class), any(String.class), any(ContributionDTO.class));
  }

  @Test
  void shouldCompleteContribution() {
    var contribution = createContribution();
    var centralServerId = contribution.getCentralServer().getId();

    when(repository.fetchCurrentByCentralServerId(any())).thenReturn(Optional.of(contribution));
    when(repository.save(any())).thenReturn(contribution);

    var updated = service.completeContribution(centralServerId);

    assertEquals(contribution.getId(), updated.getId());
    assertEquals(COMPLETE, updated.getStatus());
  }

  @Test
  void shouldUpdateContributionStats() {
    var contribution = createContribution();
    var centralServerId = contribution.getCentralServer().getId();
    contribution.setRecordsTotal(4343L);
    contribution.setRecordsProcessed(11L);
    contribution.setRecordsContributed(10L);

    when(repository.fetchCurrentByCentralServerId(any())).thenReturn(Optional.of(contribution));
    when(repository.save(any())).thenReturn(contribution);

    service.updateContributionStats(centralServerId, mapper.toDTO(contribution));

    ArgumentCaptor<Contribution> argument = ArgumentCaptor.forClass(Contribution.class);
    verify(repository).save(argument.capture());

    Contribution saved = argument.getValue();
    assertEquals(contribution.getRecordsTotal(), saved.getRecordsTotal());
    assertEquals(contribution.getRecordsProcessed(), saved.getRecordsProcessed());
    assertEquals(contribution.getRecordsContributed(), saved.getRecordsContributed());
  }

  @Test
  void shouldLogContributionError() {
    var error = new ContributionErrorDTO();
    error.setMessage("test msg");
    error.setRecordId(UUID.randomUUID());

    service.logContributionError(UUID.randomUUID(), error);

    verify(errorRepository).save(any(ContributionError.class));
  }

}
