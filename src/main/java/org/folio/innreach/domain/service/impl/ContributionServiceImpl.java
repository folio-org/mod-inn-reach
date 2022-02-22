package org.folio.innreach.domain.service.impl;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static org.folio.innreach.domain.dto.folio.inventorystorage.JobResponse.JobStatus.IN_PROGRESS;
import static org.folio.innreach.domain.entity.Contribution.Status.CANCELLED;
import static org.folio.innreach.domain.entity.Contribution.Status.COMPLETE;
import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
import static org.folio.innreach.dto.MappingValidationStatusDTO.VALID;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationRequest;
import org.folio.innreach.domain.dto.folio.inventorystorage.JobResponse;
import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.dto.ContributionErrorDTO;
import org.folio.innreach.dto.ContributionsDTO;
import org.folio.innreach.mapper.ContributionMapper;
import org.folio.innreach.repository.CentralServerRepository;
import org.folio.innreach.repository.ContributionErrorRepository;
import org.folio.innreach.repository.ContributionRepository;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;

@Log4j2
@RequiredArgsConstructor
@Service
public class ContributionServiceImpl implements ContributionService, BeanFactoryAware {

  private final ContributionRepository repository;
  private final ContributionErrorRepository errorRepository;
  private final ContributionMapper mapper;
  private final ContributionValidationService validationService;
  private final FolioExecutionContext folioContext;
  private final CentralServerRepository centralServerRepository;
  private final InstanceStorageClient client;

  private BeanFactory beanFactory;
  private ContributionJobRunner jobRunner;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  @Override
  public ContributionDTO getCurrent(UUID centralServerId) {
    var contribution = repository.fetchCurrentByCentralServerId(centralServerId)
      .map(mapper::toDTO)
      .orElseGet(ContributionDTO::new);

    contribution.setLocationsMappingStatus(validationService.getLocationMappingStatus(centralServerId));
    contribution.setItemTypeMappingStatus(validationService.getItemTypeMappingStatus(centralServerId));

    return contribution;
  }

  @Transactional
  @Override
  public ContributionDTO completeContribution(UUID contributionId) {
    var entity = fetchById(contributionId);

    entity.setStatus(COMPLETE);

    return mapper.toDTO(repository.save(entity));
  }

  @Transactional
  @Override
  public void cancelAll() {
    findAllInProgress().forEach(c -> c.setStatus(CANCELLED));
  }

  @Override
  public ContributionsDTO getHistory(UUID centralServerId, int offset, int limit) {
    var page = repository.fetchHistoryByCentralServerId(centralServerId, new OffsetRequest(offset, limit));
    return mapper.toDTOCollection(page);
  }

  @Transactional
  @Override
  public void updateContributionStats(UUID contributionId, ContributionDTO contribution) {
    var entity = fetchById(contributionId);


    Long total = defaultIfNull(contribution.getRecordsTotal(), entity.getRecordsTotal());
    Long processed = defaultIfNull(contribution.getRecordsProcessed(), entity.getRecordsProcessed());
    Long contributed = defaultIfNull(contribution.getRecordsContributed(), entity.getRecordsContributed());
    Long updated = defaultIfNull(contribution.getRecordsUpdated(), entity.getRecordsUpdated());
    Long decontributed = defaultIfNull(contribution.getRecordsDecontributed(), entity.getRecordsDecontributed());

    entity.setRecordsTotal(total);
    entity.setRecordsProcessed(processed);
    entity.setRecordsContributed(contributed);
    entity.setRecordsUpdated(updated);
    entity.setRecordsDecontributed(decontributed);
  }

  @Override
  public void startInitialContribution(UUID centralServerId) {
    log.info("Starting initial contribution for central server = {}", centralServerId);

    var existingContribution = repository.fetchCurrentByCentralServerId(centralServerId);
    if (existingContribution.isPresent()) {
      log.warn("Initial contribution is already in progress");
      throw new IllegalArgumentException("Initial contribution is already in progress");
    }

    var contribution = createEmptyContribution(centralServerId);

    log.info("Validating contribution settings");
    validateContribution(centralServerId);

    log.info("Triggering inventory instance iteration");
    var iterationJobResponse = triggerInstanceIteration();
    contribution.setJobId(iterationJobResponse.getId());

    repository.save(contribution);

    runInitialContributionJob(centralServerId, contribution);

    log.info("Initial contribution process started");
  }

  @Override
  public ContributionDTO createOngoingContribution(UUID centralServerId) {
    var contribution = createEmptyContribution(centralServerId);
    contribution.setOngoing(true);

    return mapper.toDTO(repository.save(contribution));
  }

  @Transactional
  @Override
  public void logContributionError(UUID contributionId, ContributionErrorDTO errorDTO) {
    var contribution = new Contribution();
    contribution.setId(contributionId);

    var error = mapper.toEntity(errorDTO);
    error.setContribution(contribution);

    errorRepository.save(error);
  }


  private void runInitialContributionJob(UUID centralServerId, Contribution contribution) {
    var jobRunner = getJobRunner();

    jobRunner.runInitialContributionAsync(centralServerId, folioContext.getTenantId(), contribution.getId(), contribution.getJobId());
  }

  private ContributionJobRunner getJobRunner() {
    if (jobRunner == null) {
      jobRunner = beanFactory.getBean(ContributionJobRunner.class);
    }

    return jobRunner;
  }

  private Contribution fetchById(UUID contributionId) {
    return repository.findById(contributionId)
      .orElseThrow(() -> new IllegalArgumentException("Contribution is not found by id: " + contributionId));
  }

  private List<Contribution> findAllInProgress() {
    return repository.findAllByStatus(Contribution.Status.IN_PROGRESS);
  }

  private JobResponse triggerInstanceIteration() {
    var request = createInstanceIterationRequest();

    var iterationJob = client.startInitialContribution(request);
    Assert.isTrue(iterationJob.getStatus() == IN_PROGRESS, "Unexpected iteration job status received: " + iterationJob.getStatus());

    return iterationJob;
  }

  private void validateContribution(UUID centralServerId) {
    var itemTypeMappingStatus = validationService.getItemTypeMappingStatus(centralServerId);
    Assert.isTrue(itemTypeMappingStatus == VALID, "Invalid item types mapping status");

    var locationMappingStatus = validationService.getLocationMappingStatus(centralServerId);
    Assert.isTrue(locationMappingStatus == VALID, "Invalid locations mapping status");
  }

  private InstanceIterationRequest createInstanceIterationRequest() {
    var request = new InstanceIterationRequest();
    request.setTopicName("inventory.instance-contribution");
    return request;
  }

  private Contribution createEmptyContribution(UUID centralServerId) {
    var contribution = new Contribution();
    contribution.setStatus(Contribution.Status.IN_PROGRESS);
    contribution.setRecordsTotal(0L);
    contribution.setRecordsProcessed(0L);
    contribution.setRecordsContributed(0L);
    contribution.setRecordsUpdated(0L);
    contribution.setRecordsDecontributed(0L);
    contribution.setCentralServer(centralServerRef(centralServerId));
    return contribution;
  }

}
