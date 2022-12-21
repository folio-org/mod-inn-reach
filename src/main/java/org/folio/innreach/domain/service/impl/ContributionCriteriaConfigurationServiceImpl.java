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
    log.debug("createCriteria:: parameters centralServerId: {}, dto: {}", centralServerId, dto);
    var entity = mapper.toEntity(dto);
    initId(entity);
    entity.setCentralServer(centralServerRef(centralServerId));

    var saved = repository.save(entity);

    log.info("createCriteria:: result: {}", mapper.toDTO(saved));
    return mapper.toDTO(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public ContributionCriteriaDTO getCriteria(UUID centralServerId) {
    log.debug("getCriteria:: parameters centralServerId: {}", centralServerId);
    var criteria = findCriteria(centralServerId);

    log.info("getCriteria:: result: {}", mapper.toDTO(criteria));
    return mapper.toDTO(criteria);
  }

  @Override
  public ContributionCriteriaDTO updateCriteria(UUID centralServerId, ContributionCriteriaDTO dto) {
    log.debug("updateCriteria:: parameters centralServerId: {}, dto: {}", centralServerId, dto);
    var criteria = findCriteria(centralServerId);

    criteria.setContributeAsSystemOwnedCodeId(dto.getContributeAsSystemOwnedId());
    criteria.setContributeButSuppressCodeId(dto.getContributeButSuppressId());
    criteria.setDoNotContributeCodeId(dto.getDoNotContributeId());

    merge(dto.getLocationIds(), criteria.getExcludedLocationIds(),
        criteria::addExcludedLocationId, nothing(), criteria::removeExcludedLocationId);

    log.info("updateCriteria:: result: {}", mapper.toDTO(criteria));
    return mapper.toDTO(criteria);
  }

  @Override
  public void deleteCriteria(UUID centralServerId) {
    log.debug("deleteCriteria:: parameters centralServerId: {}", centralServerId);
    var criteria = findCriteria(centralServerId);
    repository.delete(criteria);
    log.info("deleteCriteria:: Criteria deleted");
  }

  private ContributionCriteriaConfiguration findCriteria(UUID centralServerId) {
    log.debug("findCriteria:: parameters centralServerId: {}", centralServerId);
    return repository.findOne(exampleWithServerId(centralServerId))
        .orElseThrow(() -> new EntityNotFoundException("Contribution criteria not found: " +
            "centralServerId = " + centralServerId));
  }

  private static Example<ContributionCriteriaConfiguration> exampleWithServerId(UUID centralServerId) {
    log.debug("exampleWithServerId:: parameters centralServerId: {}", centralServerId);
    var toFind = new ContributionCriteriaConfiguration();
    toFind.setCentralServer(centralServerRef(centralServerId));

    log.info("exampleWithServerId:: result: {}", Example.of(toFind));
    return Example.of(toFind);
  }

}
