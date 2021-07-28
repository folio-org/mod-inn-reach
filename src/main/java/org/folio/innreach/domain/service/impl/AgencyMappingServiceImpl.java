package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.entity.AgencyLocationAcMapping;
import org.folio.innreach.domain.entity.AgencyLocationLscMapping;
import org.folio.innreach.domain.entity.AgencyLocationMapping;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.AgencyMappingService;
import org.folio.innreach.dto.AgencyLocationMappingDTO;
import org.folio.innreach.mapper.AgencyLocationMappingMapper;
import org.folio.innreach.repository.AgencyLocationMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.folio.innreach.domain.service.impl.ServiceUtils.merge;

@RequiredArgsConstructor
@Service
@Transactional
public class AgencyMappingServiceImpl implements AgencyMappingService {

  private static final String NOT_FOUND_MESSAGE_FMT = "Agency mapping with central server id: %s not found";
  private static final Comparator<AgencyLocationLscMapping> LSC_COMPARATOR = Comparator.comparing(AgencyLocationLscMapping::getLocalServerCode);
  private static final Comparator<AgencyLocationAcMapping> AC_COMPARATOR = Comparator.comparing(AgencyLocationAcMapping::getAgencyCode);

  @Autowired
  private AgencyLocationMappingRepository repository;

  @Autowired
  private AgencyLocationMappingMapper mapper;

  @Override
  public AgencyLocationMappingDTO getMapping(UUID centralServerId) {
    return fetchOne(centralServerId)
      .map(mapper::toDTO)
      .orElseThrow(() -> new EntityNotFoundException(String.format(NOT_FOUND_MESSAGE_FMT, centralServerId)));
  }

  @Override
  public AgencyLocationMappingDTO updateMapping(UUID centralServerId, AgencyLocationMappingDTO mappingDto) {
    var incoming = mapper.toEntityWithRefs(mappingDto, centralServerId);

    var updated = fetchOne(centralServerId)
      .map(mergeFunc(incoming))
      .orElse(incoming);

    repository.saveAndFlush(updated);

    return mapper.toDTO(updated);
  }

  private Optional<AgencyLocationMapping> fetchOne(UUID centralServerId) {
    return repository.fetchOneByCsId(centralServerId);
  }

  private Function<AgencyLocationMapping, AgencyLocationMapping> mergeFunc(AgencyLocationMapping incoming) {
    return existing -> {
      existing.setLibraryId(incoming.getLibraryId());
      existing.setLocationId(incoming.getLocationId());

      merge(incoming.getLocalServerMappings(), existing.getLocalServerMappings(), LSC_COMPARATOR,
        existing::addLocalServerMapping, this::updateLscMapping, nothing());

      return existing;
    };
  }

  private void updateLscMapping(AgencyLocationLscMapping incoming, AgencyLocationLscMapping existing) {
    existing.setLocationId(incoming.getLocationId());
    existing.setLibraryId(incoming.getLibraryId());

    Set<AgencyLocationAcMapping> agencyCodeMappings = incoming.getAgencyCodeMappings();
    Set<AgencyLocationAcMapping> existingAgencyCodeMappings = existing.getAgencyCodeMappings();

    merge(agencyCodeMappings, existingAgencyCodeMappings, AC_COMPARATOR,
      existing::addAgencyCodeMapping, this::updateAcMapping, existing::removeAgencyCodeMapping);
  }

  private void updateAcMapping(AgencyLocationAcMapping incoming, AgencyLocationAcMapping existing) {
    existing.setLocationId(incoming.getLocationId());
    existing.setLibraryId(incoming.getLibraryId());
  }

  private <E> Consumer<E> nothing() {
    return (e) -> { };
  }

}
