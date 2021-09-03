package org.folio.innreach.domain.service.impl;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import static org.folio.innreach.domain.dto.folio.inventorystorage.JobResponse.JobStatus.IN_PROGRESS;
import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
import static org.folio.innreach.dto.MappingValidationStatusDTO.VALID;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.client.RequestStorageClient;
import org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationRequest;
import org.folio.innreach.domain.dto.folio.inventorystorage.JobResponse;
import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.ItemContributionOptionsConfigurationService;
import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.dto.ContributionErrorDTO;
import org.folio.innreach.dto.ContributionsDTO;
import org.folio.innreach.dto.ItemContributionOptionsConfigurationDTO;
import org.folio.innreach.mapper.ContributionMapper;
import org.folio.innreach.repository.ContributionErrorRepository;
import org.folio.innreach.repository.ContributionRepository;

@Log4j2
@AllArgsConstructor
@Service
public class ContributionServiceImpl implements ContributionService {

  private final ContributionRepository repository;
  private final ContributionErrorRepository errorRepository;
  private final ContributionMapper mapper;
  private final ContributionValidationService validationService;
  private final ContributionJobRunner jobRunner;

  private final InstanceStorageClient client;

  private final ItemContributionOptionsConfigurationService itemContributionOptionsConfigurationService;

  private final InventoryClient inventoryClient;
  private final RequestStorageClient requestStorageClient;

  @Override
  public ContributionDTO getCurrent(UUID centralServerId) {
    var entity = repository.fetchCurrentByCentralServerId(centralServerId)
      .orElseGet(Contribution::new);

    var contribution = mapper.toDTO(entity);

    validationService.validate(centralServerId, contribution);

    return contribution;
  }

  @Override
  public void completeContribution(UUID centralServerId) {
    var entity = findCurrent(centralServerId);

    entity.setStatus(Contribution.Status.COMPLETE);

    repository.save(entity);
  }

  @Override
  public ContributionsDTO getHistory(UUID centralServerId, int offset, int limit) {
    var page = repository.fetchHistoryByCentralServerId(centralServerId, PageRequest.of(offset, limit));
    return mapper.toDTOCollection(page);
  }

  @Transactional(propagation = REQUIRES_NEW)
  @Override
  public void updateContributionStats(UUID centralServerId, ContributionDTO contribution) {
    var entity = findCurrent(centralServerId);

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

    repository.save(entity);
  }

  @Override
  public void startInitialContribution(UUID centralServerId) {
    log.info("Starting initial contribution for central server = {}", centralServerId);

    var existingContribution = repository.fetchCurrentByCentralServerId(centralServerId);
    Assert.isTrue(existingContribution.isEmpty(), "Initial contribution is already in progress");

    var contribution = createEmptyContribution(centralServerId);

    log.info("Validating contribution settings");
    validateContribution(centralServerId, contribution);

    log.info("Triggering inventory instance iteration");
    var iterationJobResponse = triggerInstanceIteration();
    contribution.setJobId(iterationJobResponse.getId());

    repository.save(contribution);

    jobRunner.run(centralServerId, mapper.toDTO(contribution));

    log.info("Initial contribution process started");
  }

  @Transactional(propagation = REQUIRES_NEW)
  @Override
  public void logContributionError(UUID contributionId, ContributionErrorDTO errorDTO) {
    var contribution = new Contribution();
    contribution.setId(contributionId);

    var error = mapper.toEntity(errorDTO);
    error.setContribution(contribution);

    errorRepository.save(error);
  }

  private Contribution findCurrent(UUID centralServerId) {
    return repository.fetchCurrentByCentralServerId(centralServerId)
      .orElseThrow(() -> new IllegalArgumentException("Initial contribution is not found for central server = " + centralServerId));
  }

  private JobResponse triggerInstanceIteration() {
    var request = createInstanceIterationRequest();

    var iterationJob = startIterationMocked(request);
    Assert.isTrue(iterationJob.getStatus() == IN_PROGRESS, "Unexpected iteration job status received: " + iterationJob.getStatus());

    return iterationJob;
  }

  private void validateContribution(UUID centralServerId, Contribution contribution) {
    var dto = mapper.toDTO(contribution);
    validationService.validate(centralServerId, dto);

    Assert.isTrue(dto.getItemTypeMappingStatus() == VALID, "Invalid item types mapping status");
    Assert.isTrue(dto.getLocationsMappingStatus() == VALID, "Invalid locations mapping status");
  }

  private JobResponse startIterationMocked(InstanceIterationRequest request) {
    try {
      return client.startInitialContribution(request);
    } catch (Exception e) {
      log.warn("mod-inventory-storage Iteration endpoint is yet to be implemented. Returning stubbed response..");

      return JobResponse.builder()
        .id(UUID.randomUUID())
        .status(JobResponse.JobStatus.IN_PROGRESS)
        .numberOfRecordsPublished(0)
        .submittedDate(OffsetDateTime.now())
        .build();
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

  @Override
  public ContributionItemCirculationStatus getItemCirculationStatus(UUID centralServerId, UUID itemId) {
    var itemContributionConfig = itemContributionOptionsConfigurationService
      .getItmContribOptConf(centralServerId);

    var inventoryItem = inventoryClient.getItemById(itemId);

    if (isItemNonLendable(inventoryItem, itemContributionConfig)) {
      return ContributionItemCirculationStatus.NON_LENDABLE;
    }

    if (inventoryItem.getStatus().isCheckedOut()) {
      return ContributionItemCirculationStatus.ON_LOAN;
    }

    if (isItemAvailableForContribution(inventoryItem, itemContributionConfig)) {
      return ContributionItemCirculationStatus.AVAILABLE;
    }

    return ContributionItemCirculationStatus.NOT_AVAILABLE;
  }

  private boolean isItemNonLendable(InventoryItemDTO inventoryItem,
                                    ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    return isItemNonLendableByLoanTypes(inventoryItem, itemContributionConfig) ||
      isItemNonLendableByLocations(inventoryItem, itemContributionConfig) ||
      isItemNonLendableByMaterialTypes(inventoryItem, itemContributionConfig);
  }

  private boolean isItemNonLendableByLoanTypes(InventoryItemDTO inventoryItem,
                                               ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    var nonLendableLoanTypes = itemContributionConfig.getNonLendableLoanTypes();
    return nonLendableLoanTypes.contains(inventoryItem.getPermanentLoanType().getId()) ||
      nonLendableLoanTypes.contains(inventoryItem.getTemporaryLoanType().getId());
  }

  private boolean isItemNonLendableByLocations(InventoryItemDTO inventoryItem,
                                               ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    var nonLendableLocations = itemContributionConfig.getNonLendableLocations();
    return nonLendableLocations.contains(inventoryItem.getPermanentLocation().getId());
  }

  private boolean isItemNonLendableByMaterialTypes(InventoryItemDTO inventoryItem,
                                                   ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    var nonLendableMaterialTypes = itemContributionConfig.getNonLendableMaterialTypes();
    return nonLendableMaterialTypes.contains(inventoryItem.getMaterialType().getId());
  }

  private boolean isItemAvailableForContribution(InventoryItemDTO inventoryItem,
                                                 ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    var itemStatus = inventoryItem.getStatus();

    if (itemStatus.isInTransit() && isItemRequested(inventoryItem)) {
      return false;
    }

    return itemStatus.isAvailable() || !itemContributionConfig.getNotAvailableItemStatuses().contains(itemStatus.getName());
  }

  private boolean isItemRequested(InventoryItemDTO inventoryItem) {
    var itemRequests = requestStorageClient.findRequests(inventoryItem.getId());
    return itemRequests.getTotalRecords() != 0;
  }

}
