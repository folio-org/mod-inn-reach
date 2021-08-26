package org.folio.innreach.domain.service.impl;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.dto.folio.inventoryStorage.InstanceIterationEvent;
import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.client.MaterialTypesClient;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationRequest;
import org.folio.innreach.domain.dto.folio.inventorystorage.JobResponse;
import org.folio.innreach.domain.dto.folio.inventorystorage.MaterialTypeDTO;
import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.dto.ContributionsDTO;
import org.folio.innreach.mapper.ContributionMapper;
import org.folio.innreach.repository.ContributionRepository;

@Log4j2
@AllArgsConstructor
@Service
public class ContributionServiceImpl implements ContributionService {

  private final ContributionRepository repository;
  private final ContributionMapper mapper;
  private final ContributionValidationService validationService;

  private final InstanceStorageClient client;

  @Override
  public ContributionDTO getCurrent(UUID centralServerId) {
    var entity = repository.fetchCurrentByCentralServerId(centralServerId)
      .orElseGet(Contribution::new);

    var contribution = mapper.toDTO(entity);

    validationService.validate(centralServerId, contribution);

    return contribution;
  }

  @Override
  public ContributionsDTO getHistory(UUID centralServerId, int offset, int limit) {
    var page = repository.fetchHistoryByCentralServerId(centralServerId, PageRequest.of(offset, limit));
    return mapper.toDTOCollection(page);
  }

  @Override
  public void contributeInstances(List<InstanceIterationEvent> events) {
    // TODO: implement when D2IR contribution API client is ready
  }

  @Override
  public void startInitialContribution(UUID centralServerId) {
    var contribution = createEmptyContribution(centralServerId);

    var request = createInstanceIterationRequest();
    log.info("Calling mod-inventory storage...");

    JobResponse jobResponse = startIterationMocked(request);

    contribution.setJobId(jobResponse.getId());
    repository.save(contribution);

    log.info("Initial contribution process started.");
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
}
