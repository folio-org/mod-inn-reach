package org.folio.innreach.domain.service.impl;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.folio.innreach.domain.dto.folio.inventorystorage.JobResponse.JobStatus.IN_PROGRESS;
import static org.folio.innreach.domain.entity.Contribution.Status.CANCELLED;
import static org.folio.innreach.domain.entity.Contribution.Status.COMPLETE;
import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
import static org.folio.innreach.dto.MappingValidationStatusDTO.INVALID;
import static org.folio.innreach.dto.MappingValidationStatusDTO.VALID;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.batch.contribution.InitialContributionJobConsumerContainer;
import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.domain.exception.InitialContributionStatusValidationException;
import org.folio.innreach.external.exception.InnReachTimeOutException;
import org.folio.spring.config.properties.FolioEnvironment;
import org.springframework.beans.factory.BeanFactory;
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

  @Override
  public ContributionDTO getCurrent(UUID centralServerId) {
    var contribution = repository.fetchCurrentByCentralServerId(centralServerId)
      .map(mapper::toDTO)
      .orElseGet(ContributionDTO::new);

    try {
      contribution.setLocationsMappingStatus(validationService.getLocationMappingStatus(centralServerId));
      contribution.setItemTypeMappingStatus(validationService.getItemTypeMappingStatus(centralServerId));
    } catch (Exception e) {
      log.warn("getCurrent:: Can't validate location mappings", e);
      contribution.setLocationsMappingStatus(INVALID);
      contribution.setItemTypeMappingStatus(INVALID);
    }

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
    log.debug("updateContributionStats:: parameters contributionId: {}, contribution: {}", contributionId, contribution);
    var entity = fetchById(contributionId);

    Long total = getIfNull(contribution.getRecordsTotal(), entity.getRecordsTotal());
    Long processed = getIfNull(contribution.getRecordsProcessed(), entity.getRecordsProcessed());
    Long contributed = getIfNull(contribution.getRecordsContributed(), entity.getRecordsContributed());
    Long updated = getIfNull(contribution.getRecordsUpdated(), entity.getRecordsUpdated());
    Long decontributed = getIfNull(contribution.getRecordsDecontributed(), entity.getRecordsDecontributed());

    entity.setRecordsTotal(total);
    entity.setRecordsProcessed(processed);
    entity.setRecordsContributed(contributed);
    entity.setRecordsUpdated(updated);
    entity.setRecordsDecontributed(decontributed);
  }

  @Override
  public void startInitialContribution(UUID centralServerId) {
    log.info("Starting initial contribution for central server: {}", centralServerId);

    var existingContribution = repository.fetchCurrentByCentralServerId(centralServerId);
    if (existingContribution.isPresent()) {
      log.warn("Initial contribution is already in progress");
      throw new IllegalArgumentException("Initial contribution is already in progress");
    }

    var contribution = createEmptyContribution(centralServerId);

    validateContribution(centralServerId);

    var iterationJobResponse = triggerInstanceIteration();
    var numberOfRecords = iterationJobResponse.getNumberOfRecordsPublished();
    contribution.setJobId(iterationJobResponse.getId());
    contribution.setRecordsTotal(numberOfRecords.longValue());
    var saved = repository.save(contribution);

    log.info("Initial contribution started with contribution id: {} and job id: {}",
      saved.getId(), iterationJobResponse.getId());
  }

  @Override
  @Transactional
  public void updateStatisticsAndContributionStatus() {
    repository.updateStatisticsByCentralServerId().ifPresent(contribution ->
      log.info("updateStatisticsAndContributionStatus:: recordsTotal - {}, recordsProcessed - {}, recordsContributed - {}",
        contribution.getRecordsTotal(), contribution.getRecordsProcessed(), contribution.getRecordsContributed()));
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

    var iterationJob = instanceStorageClient.startInstanceIteration(request);
    Assert.isTrue(iterationJob.getStatus() == IN_PROGRESS, "Unexpected iteration job status received: " + iterationJob.getStatus());

    return iterationJob;
  }

  private void cancelInstanceIteration(Contribution contribution) {
    var iterationJobId = contribution.getJobId();
    try {
      instanceStorageClient.cancelInstanceIteration(iterationJobId);
    } catch (Exception e) {
      log.info("Unable to cancel instance iteration job {}", iterationJobId, e);
    }
  }

  private void validateContribution(UUID centralServerId) {
    try {
      var itemTypeMappingStatus = validationService.getItemTypeMappingStatus(centralServerId);
      var locationMappingStatus = validationService.getLocationMappingStatus(centralServerId);

      if (itemTypeMappingStatus == INVALID) {
        log.warn("validateContribution:: Contribution validation failed for central server {}, itemTypeMappingStatus: {}",
          centralServerId, itemTypeMappingStatus);
        throw new InitialContributionStatusValidationException("Contribution validation failed. Please fix item type mapping issues before starting contribution");
      }

      if (locationMappingStatus == INVALID) {
        log.warn("validateContribution:: Contribution validation failed for central server {}, locationMappingStatus: {}",
          centralServerId, locationMappingStatus);
        throw new InitialContributionStatusValidationException("Contribution validation failed. Please fix location mapping issues before starting contribution");
      }
    } catch (InnReachTimeOutException ex) {
      log.warn("validateContribution:: Timeout occurred while validating contribution for central server {}", centralServerId, ex);
      throw new InitialContributionStatusValidationException("Failed to validate contribution status: %s. Please try again later.".formatted(ex.getMessage()));
    } catch (Exception ex) {
      throw new InitialContributionStatusValidationException("Failed to validate contribution status.", ex);
    }
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
