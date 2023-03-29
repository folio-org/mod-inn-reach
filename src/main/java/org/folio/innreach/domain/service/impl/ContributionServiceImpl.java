package org.folio.innreach.domain.service.impl;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static org.folio.innreach.domain.dto.folio.inventorystorage.JobResponse.JobStatus.IN_PROGRESS;
import static org.folio.innreach.domain.entity.Contribution.Status.CANCELLED;
import static org.folio.innreach.domain.entity.Contribution.Status.COMPLETE;
import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
import static org.folio.innreach.dto.MappingValidationStatusDTO.VALID;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.batch.contribution.InitialContributionJobConsumerContainer;
import org.folio.innreach.batch.contribution.IterationEventReaderFactory;
import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.config.props.FolioEnvironment;
import org.folio.innreach.external.exception.InnReachException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

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
import org.folio.innreach.repository.ContributionErrorRepository;
import org.folio.innreach.repository.ContributionRepository;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;

@Log4j2
@RequiredArgsConstructor
@Service
public class ContributionServiceImpl implements ContributionService {

  public static final String COMPLETED = "COMPLETED";
  private final ContributionRepository repository;
  private final ContributionErrorRepository errorRepository;
  private final ContributionMapper mapper;
  private final ContributionValidationService validationService;
  private final FolioExecutionContext folioContext;
  private final InstanceStorageClient instanceStorageClient;
  private final BeanFactory beanFactory;
  private final FolioEnvironment folioEnv;
  private final ContributionJobProperties jobProperties;


  private ContributionJobRunner jobRunner;

  @Qualifier("contributionRetryTemplate")
  private final RetryTemplate retryTemplate;

  @Override
  public ContributionDTO getCurrent(UUID centralServerId) {
    log.debug("getCurrent:: parameters centralServerId: {}", centralServerId);
    var contribution = repository.fetchCurrentByCentralServerId(centralServerId)
      .map(mapper::toDTO)
      .orElseGet(ContributionDTO::new);

    contribution.setLocationsMappingStatus(validationService.getLocationMappingStatus(centralServerId));
    contribution.setItemTypeMappingStatus(validationService.getItemTypeMappingStatus(centralServerId));

    log.info("getCurrent:: result: {}", contribution);
    return contribution;
  }

  @Transactional
  @Override
  public ContributionDTO completeContribution(UUID contributionId) {
    log.debug("completeContribution:: parameters contributionId: {}", contributionId);
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
    log.debug("getHistory:: parameters centralServerId: {}, offset: {}, limit: {}", centralServerId, offset, limit);
    var page = repository.fetchHistoryByCentralServerId(centralServerId, new OffsetRequest(offset, limit));
    log.info("getHistory:: result: {}", mapper.toDTOCollection(page));
    return mapper.toDTOCollection(page);
  }

  @Transactional
  @Override
  public void updateContributionStats(UUID contributionId, ContributionDTO contribution) {
    log.debug("updateContributionStats:: parameters contributionId: {}, contribution: {}", contributionId, contribution);
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
    log.info("updateContributionStats:: Contribution stats updated");
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
    var numberOfRecords = iterationJobResponse.getNumberOfRecordsPublished();
    log.info("numberOfRecords from iterationJobResponse-> {}",numberOfRecords);

    JobResponse updatedJobResponse = retryTemplate.execute(r -> getJobResponse(iterationJobResponse.getId()));

    if(updatedJobResponse!=null) {
      log.info("numberOfRecords from updatedJobResponse-> {}",updatedJobResponse.getNumberOfRecordsPublished());
      numberOfRecords = updatedJobResponse.getNumberOfRecordsPublished();
    }

    contribution.setJobId(iterationJobResponse.getId());

    repository.save(contribution);

    runInitialContributionJob(centralServerId, contribution, numberOfRecords);

    log.info("Initial contribution process started");
  }

  private JobResponse getJobResponse(UUID id) {

    JobResponse responseAfterTrigger = instanceStorageClient.getJobById(id);

    if (responseAfterTrigger != null) {
      log.info("responseAfterTrigger: status: {}", responseAfterTrigger.getStatus().toString());
      log.info("responseAfterTrigger: number: {}", responseAfterTrigger.getNumberOfRecordsPublished());
      log.info("responseAfterTrigger: id: {}", responseAfterTrigger.getId());
      if(!COMPLETED.equalsIgnoreCase(responseAfterTrigger.getStatus().toString())) {
        log.info("not completed yet");
        throw new InnReachException("record is still not there->>"+responseAfterTrigger.getNumberOfRecordsPublished());
      }
    }
    else {
      log.info("responseAfterTrigger is null");
      throw new InnReachException("responseAfterTrigger is null");
    }

    return responseAfterTrigger;

  }

  @Override
  public ContributionDTO createOngoingContribution(UUID centralServerId) {
    log.debug("createOngoingContribution:: parameters centralServerId: {}", centralServerId);
    var contribution = createEmptyContribution(centralServerId);
    contribution.setOngoing(true);

    return mapper.toDTO(repository.save(contribution));
  }

  @Transactional
  @Override
  public void logContributionError(UUID contributionId, ContributionErrorDTO errorDTO) {
    log.debug("logContributionError:: parameters contributionId: {}, errorDTO: {}", contributionId, errorDTO);
    var contribution = new Contribution();
    contribution.setId(contributionId);

    var error = mapper.toEntity(errorDTO);
    error.setContribution(contribution);

    errorRepository.save(error);
  }

  @Transactional
  @Override
  public void cancelCurrent(UUID centralServerId) {
    log.info("cancelCurrent:: parameters centralServerId: {}", centralServerId);
    repository.fetchCurrentByCentralServerId(centralServerId).ifPresent(contribution -> {
      log.info("Cancelling initial contribution for central server {}", centralServerId);

      cancelInstanceIteration(contribution);

      getJobRunner().cancelInitialContribution(contribution.getId());

      contribution.setStatus(CANCELLED);
    });

    var topic = String.format("%s.%s.%s",
      folioEnv.getEnvironment(), folioContext.getTenantId(), jobProperties.getReaderTopic());

    InitialContributionJobConsumerContainer.stopConsumer(topic);

  }

  private void runInitialContributionJob(UUID centralServerId, Contribution contribution, Integer numberOfRecords) {
    log.debug("runInitialContributionJob:: parameters centralServerId: {}, contribution: {}", centralServerId, contribution);
    getJobRunner().startInitialContribution(centralServerId, folioContext.getTenantId(), contribution.getId(), contribution.getJobId(), numberOfRecords);
  }

  private ContributionJobRunner getJobRunner() {
    if (jobRunner == null) {
      jobRunner = beanFactory.getBean(ContributionJobRunner.class);
    }

    return jobRunner;
  }

  private Contribution fetchById(UUID contributionId) {
    log.debug("fetchById:: parameters contributionId: {}", contributionId);
    return repository.findById(contributionId)
      .orElseThrow(() -> new IllegalArgumentException("Contribution is not found by id: " + contributionId));
  }

  private List<Contribution> findAllInProgress() {
    return repository.findAllByStatus(Contribution.Status.IN_PROGRESS);
  }

  private JobResponse triggerInstanceIteration() {
    log.debug("triggerInstanceIteration:: no parameter");
    var request = createInstanceIterationRequest();

    var iterationJob = instanceStorageClient.startInstanceIteration(request);
    Assert.isTrue(iterationJob.getStatus() == IN_PROGRESS, "Unexpected iteration job status received: " + iterationJob.getStatus());

    log.info("triggerInstanceIteration: result: {}", iterationJob.toString());
    log.info("triggerInstanceIteration: message published number: {}", iterationJob.getNumberOfRecordsPublished());
    log.info("triggerInstanceIteration: message published status: {}", iterationJob.getStatus().toString());
    log.info("triggerInstanceIteration: message published id: {}", iterationJob.getId());
    return iterationJob;
  }

  private void cancelInstanceIteration(Contribution contribution) {
    log.info("cancelInstanceIteration:: parameters contribution: {}", contribution);
    var iterationJobId = contribution.getJobId();
    try {
      instanceStorageClient.cancelInstanceIteration(iterationJobId);
    } catch (Exception e) {
      log.info("Unable to cancel instance iteration job {}", iterationJobId, e);
    }
  }

  private void validateContribution(UUID centralServerId) {
    log.debug("validateContribution:: parameters centralServerId: {}", centralServerId);
    var itemTypeMappingStatus = validationService.getItemTypeMappingStatus(centralServerId);
    Assert.isTrue(itemTypeMappingStatus == VALID, "Invalid item types mapping status");

    var locationMappingStatus = validationService.getLocationMappingStatus(centralServerId);
    Assert.isTrue(locationMappingStatus == VALID, "Invalid locations mapping status");
  }

  private InstanceIterationRequest createInstanceIterationRequest() {
    log.debug("createInstanceIterationRequest:: no parameter");
    var request = new InstanceIterationRequest();
    request.setTopicName("inventory.instance-contribution");
    log.info("createInstanceIterationRequest:: result: {}", request);
    return request;
  }

  private Contribution createEmptyContribution(UUID centralServerId) {
    log.debug("createEmptyContribution:: parameters centralServerId: {}", centralServerId);
    var contribution = new Contribution();
    contribution.setStatus(Contribution.Status.IN_PROGRESS);
    contribution.setRecordsTotal(0L);
    contribution.setRecordsProcessed(0L);
    contribution.setRecordsContributed(0L);
    contribution.setRecordsUpdated(0L);
    contribution.setRecordsDecontributed(0L);
    contribution.setCentralServer(centralServerRef(centralServerId));
    log.info("createEmptyContribution:: result: {}", contribution);
    return contribution;
  }

}
