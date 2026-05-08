package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.fixture.JobResponseFixture.updateJobResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.dto.ContributionDTO.StatusEnum.COMPLETE;
import static org.folio.innreach.dto.MappingValidationStatusDTO.INVALID;
import static org.folio.innreach.dto.MappingValidationStatusDTO.VALID;
import static org.folio.innreach.fixture.ContributionFixture.createContribution;
import static org.folio.innreach.fixture.JobResponseFixture.createJobResponse;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.spring.config.properties.FolioEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.BeanFactory;

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
import org.folio.spring.FolioExecutionContext;

class ContributionServiceImplTest {

  @Mock
  private ContributionRepository repository;

  @Mock
  private ContributionErrorRepository errorRepository;

  @Spy
  private InstanceStorageClient storageClient;

  @Mock
  private ContributionJobRunner jobRunner;

  @Mock
  private FolioExecutionContext context;

  @Spy
  private ContributionMapper mapper = new ContributionMapperImpl(new MappingMethods());

  @Mock
  private ContributionValidationService validationService;

  @Mock
  private BeanFactory beanFactory;

  @InjectMocks
  private ContributionServiceImpl service;

  @Mock
  private FolioEnvironment folioEnv;

  @Mock
  private ContributionJobProperties jobProperties;

  @BeforeEach
  void beforeEachSetup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void startInitialContributionProcess() {
    when(repository.save(any(Contribution.class))).thenReturn(createContribution());
    when(storageClient.startInstanceIteration(any())).thenReturn(createJobResponse());
    when(storageClient.getJobById(any())).thenReturn(updateJobResponse());
    when(validationService.getItemTypeMappingStatus(any())).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(any())).thenReturn(VALID);
    when(beanFactory.getBean(ContributionJobRunner.class)).thenReturn(jobRunner);

    service.startInitialContribution(UUID.randomUUID());

    verify(storageClient).startInstanceIteration(any());
    verify(repository).save(any(Contribution.class));

    when(storageClient.getJobById(any())).thenReturn(null);

    var jobResponse = createJobResponse();
    jobResponse.setNumberOfRecordsPublished(0);
    when(storageClient.getJobById(any())).thenReturn(jobResponse);
  }

  @Test
  void shouldCompleteContribution() {
    var contribution = createContribution();
    var centralServerId = contribution.getCentralServer().getId();

    when(repository.findById(any())).thenReturn(Optional.of(contribution));
    when(repository.save(any())).thenReturn(contribution);

    var updated = service.completeContribution(centralServerId);

    assertEquals(contribution.getId(), updated.getId());
    assertEquals(COMPLETE, updated.getStatus());
  }

  @Test
  void shouldCreateNewOngoingContribution() {
    var contribution = createContribution();
    var centralServerId = contribution.getCentralServer().getId();
    when(repository.save(any())).thenReturn(contribution);

    assertNotNull(service.createOngoingContribution(centralServerId));
  }


  @Test
  void shouldUpdateContributionStats() {
    var contribution = createContribution();
    contribution.setRecordsTotal(4343L);
    contribution.setRecordsProcessed(11L);
    contribution.setRecordsContributed(10L);

    when(repository.findById(any())).thenReturn(Optional.of(contribution));
    when(repository.save(any())).thenReturn(contribution);

    service.updateContributionStats(contribution.getId(), mapper.toDTO(contribution));

    Contribution saved = repository.findById(contribution.getId()).get();
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

  @Test
  void cancelAllTest(){
    when(repository.findAllByStatus(any())).thenReturn(Arrays.asList(createContribution()));

    service.cancelAll();

    verify(repository).findAllByStatus(any());
  }

  @Test
  void shouldGetCurrentContribution() {
    var contribution = createContribution();
    var centralServerId = contribution.getCentralServer().getId();

    when(repository.fetchCurrentByCentralServerId(centralServerId)).thenReturn(Optional.of(contribution));
    when(validationService.getItemTypeMappingStatus(centralServerId)).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(centralServerId)).thenReturn(VALID);

    var result = service.getCurrent(centralServerId);

    assertNotNull(result);
    assertEquals(VALID, result.getItemTypeMappingStatus());
    assertEquals(VALID, result.getLocationsMappingStatus());
  }

  @Test
  void shouldReturnEmptyContributionWhenNoCurrentExists() {
    var centralServerId = UUID.randomUUID();

    when(repository.fetchCurrentByCentralServerId(centralServerId)).thenReturn(Optional.empty());
    when(validationService.getItemTypeMappingStatus(centralServerId)).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(centralServerId)).thenReturn(VALID);

    var result = service.getCurrent(centralServerId);

    assertNotNull(result);
    assertEquals(VALID, result.getItemTypeMappingStatus());
  }

  @Test
  void shouldSetInvalidStatusWhenValidationFails() {
    var centralServerId = UUID.randomUUID();

    when(repository.fetchCurrentByCentralServerId(centralServerId)).thenReturn(Optional.empty());
    when(validationService.getItemTypeMappingStatus(centralServerId)).thenThrow(new RuntimeException("validation error"));

    var result = service.getCurrent(centralServerId);

    assertNotNull(result);
    assertEquals(INVALID, result.getItemTypeMappingStatus());
    assertEquals(INVALID, result.getLocationsMappingStatus());
  }

  @Test
  void shouldThrowWhenContributionAlreadyInProgress() {
    var contribution = createContribution();
    var centralServerId = contribution.getCentralServer().getId();

    when(repository.fetchCurrentByCentralServerId(centralServerId)).thenReturn(Optional.of(contribution));

    assertThrows(IllegalArgumentException.class,
      () -> service.startInitialContribution(centralServerId));
  }

  @Test
  void shouldThrowWhenItemTypeMappingInvalid() {
    var centralServerId = UUID.randomUUID();

    when(repository.fetchCurrentByCentralServerId(centralServerId)).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(validationService.getItemTypeMappingStatus(centralServerId)).thenReturn(INVALID);

    assertThrows(IllegalArgumentException.class,
      () -> service.startInitialContribution(centralServerId));
  }

  @Test
  void shouldThrowWhenLocationMappingInvalid() {
    var centralServerId = UUID.randomUUID();

    when(repository.fetchCurrentByCentralServerId(centralServerId)).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(validationService.getItemTypeMappingStatus(centralServerId)).thenReturn(VALID);
    when(validationService.getLocationMappingStatus(centralServerId)).thenReturn(INVALID);

    assertThrows(IllegalArgumentException.class,
      () -> service.startInitialContribution(centralServerId));
  }

  @Test
  void shouldThrowWhenCompleteContributionNotFound() {
    var contributionId = UUID.randomUUID();

    when(repository.findById(contributionId)).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class,
      () -> service.completeContribution(contributionId));
  }

  @Test
  void shouldThrowWhenUpdateStatsContributionNotFound() {
    var contributionId = UUID.randomUUID();

    when(repository.findById(contributionId)).thenReturn(Optional.empty());

    var contributionDTO = new ContributionDTO();
    assertThrows(IllegalArgumentException.class,
      () -> service.updateContributionStats(contributionId, contributionDTO));
  }

  @Test
  void shouldUpdateStatisticsAndContributionStatus() {
    when(repository.updateStatisticsByCentralServerId()).thenReturn(Optional.of(createContribution()));

    service.updateStatisticsAndContributionStatus();

    verify(repository).updateStatisticsByCentralServerId();
  }

  @Test
  void shouldUpdateStatisticsWhenNoContributionFound() {
    when(repository.updateStatisticsByCentralServerId()).thenReturn(Optional.empty());

    service.updateStatisticsAndContributionStatus();

    verify(repository).updateStatisticsByCentralServerId();
  }

  @Test
  void shouldCancelAllWithEmptyList() {
    when(repository.findAllByStatus(any())).thenReturn(Arrays.asList());

    service.cancelAll();

    verify(repository).findAllByStatus(any());
  }

}
