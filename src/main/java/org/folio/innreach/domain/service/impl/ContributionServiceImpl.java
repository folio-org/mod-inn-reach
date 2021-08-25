package org.folio.innreach.domain.service.impl;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.dto.folio.inventoryStorage.InstanceIterationEvent;
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

}
