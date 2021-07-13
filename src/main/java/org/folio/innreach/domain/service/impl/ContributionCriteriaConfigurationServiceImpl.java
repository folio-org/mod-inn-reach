package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
import static org.folio.innreach.domain.service.impl.ServiceUtils.initId;
import static org.folio.innreach.domain.service.impl.ServiceUtils.merge;
import static org.folio.innreach.domain.service.impl.ServiceUtils.nothing;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.ContributionCriteriaConfigurationService;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.folio.innreach.mapper.ContributionCriteriaConfigurationMapper;
import org.folio.innreach.repository.ContributionCriteriaConfigurationRepository;

@Log4j2
@RequiredArgsConstructor
@Service
@Transactional
public class ContributionCriteriaConfigurationServiceImpl implements ContributionCriteriaConfigurationService {

  private final ContributionCriteriaConfigurationRepository repository;
  private final ContributionCriteriaConfigurationMapper mapper;


  @Override
  public ContributionCriteriaDTO createCriteria(UUID centralServerId, ContributionCriteriaDTO dto) {
    var entity = mapper.toEntity(dto);
    initId(entity);
    entity.setCentralServer(centralServerRef(centralServerId));

    var saved = repository.save(entity);

    return mapper.toDTO(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public ContributionCriteriaDTO getCriteria(UUID centralServerId) {
    var criteria = findCriteria(centralServerId);

    return mapper.toDTO(criteria);
  }

  @Override
  public ContributionCriteriaDTO updateCriteria(UUID centralServerId, ContributionCriteriaDTO dto) {
    var criteria = findCriteria(centralServerId);

    criteria.setContributeAsSystemOwnedCodeId(dto.getContributeAsSystemOwnedId());
    criteria.setContributeButSuppressCodeId(dto.getContributeButSuppressId());
    criteria.setDoNotContributeCodeId(dto.getDoNotContributeId());

    merge(dto.getLocationIds(), criteria.getExcludedLocationIds(),
        criteria::addExcludedLocationId, nothing(), criteria::removeExcludedLocationId);

    return mapper.toDTO(criteria);
  }

  @Override
  public void deleteCriteria(UUID centralServerId) {
    var criteria = findCriteria(centralServerId);
    repository.delete(criteria);
  }

  private ContributionCriteriaConfiguration findCriteria(UUID centralServerId) {
    return repository.findOne(exampleWithServerId(centralServerId))
        .orElseThrow(() -> new EntityNotFoundException("Contribution criteria not found: " +
            "centralServerId = " + centralServerId));
  }

  private static Example<ContributionCriteriaConfiguration> exampleWithServerId(UUID centralServerId) {
    var toFind = new ContributionCriteriaConfiguration();
    toFind.setCentralServer(centralServerRef(centralServerId));

    return Example.of(toFind);
  }

}
