package org.folio.innreach.domain.service.impl;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.entity.AgencyLocationAcMapping;
import org.folio.innreach.domain.entity.AgencyLocationLscMapping;
import org.folio.innreach.domain.entity.AgencyLocationMapping;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.AgencyMappingService;
import org.folio.innreach.domain.service.AgencyService;
import org.folio.innreach.dto.AgencyLocationAcMappingDTO;
import org.folio.innreach.dto.AgencyLocationLscMappingDTO;
import org.folio.innreach.dto.AgencyLocationMappingDTO;
import org.folio.innreach.dto.LocalServer;
import org.folio.innreach.mapper.AgencyLocationMappingMapper;
import org.folio.innreach.repository.AgencyLocationMappingRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static org.folio.innreach.domain.service.impl.ServiceUtils.merge;

@RequiredArgsConstructor
@Service
@Transactional
public class AgencyMappingServiceImpl implements AgencyMappingService {

  private static final String NOT_FOUND_MESSAGE_FMT = "Agency mapping with central server id: %s not found";
  private static final Comparator<AgencyLocationLscMapping> LSC_COMPARATOR = Comparator.comparing(AgencyLocationLscMapping::getLocalServerCode);
  private static final Comparator<AgencyLocationAcMapping> AC_COMPARATOR = Comparator.comparing(AgencyLocationAcMapping::getAgencyCode);

  private final AgencyLocationMappingRepository repository;
  private final AgencyLocationMappingMapper mapper;
  private final AgencyService agencyService;

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

  @Override
  public UUID getLocationIdByAgencyCode(UUID centralServerId, String agencyCode) {
    var mapping = getMapping(centralServerId);

    return getLocationIdByAgencyCode(mapping, agencyCode)
      .or(() -> getLocationIdByLocalServer(mapping, agencyCode, centralServerId))
      .orElse(mapping.getLocationId());
  }

  private LocalServer getLocalServerByAgencyCode(UUID centralServerId, String agencyCode) {
    var localServers = agencyService.getLocalServerAgencies(centralServerId);
    return localServers.getLocalServerList().stream()
      .filter(ls -> isNotEmpty(ls.getAgencyList()))
      .filter(l -> l.getAgencyList().contains(agencyCode))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Central agency for code " + agencyCode + " is not found"));
  }

  private Optional<UUID> getLocationIdByLocalServer(AgencyLocationMappingDTO mapping, String agencyCode, UUID centralServerId) {
    var localServer = getLocalServerByAgencyCode(centralServerId, agencyCode);
    var localCode = localServer.getLocalCode();

    return mapping.getLocalServers()
      .stream()
      .filter(m -> localCode.equals(m.getLocalCode()))
      .map(AgencyLocationLscMappingDTO::getLocationId)
      .findFirst();
  }

  private Optional<UUID> getLocationIdByAgencyCode(AgencyLocationMappingDTO mapping, String agencyCode) {
    return mapping.getLocalServers().stream()
      .map(AgencyLocationLscMappingDTO::getAgencyCodeMappings)
      .flatMap(Collection::stream)
      .filter(m -> agencyCode.equals(m.getAgencyCode()))
      .map(AgencyLocationAcMappingDTO::getLocationId)
      .findFirst();
  }

  private Optional<AgencyLocationMapping> fetchOne(UUID centralServerId) {
    return repository.fetchOneByCsId(centralServerId);
  }

  private Function<AgencyLocationMapping, AgencyLocationMapping> mergeFunc(AgencyLocationMapping incoming) {
    return existing -> {
      existing.setLibraryId(incoming.getLibraryId());
      existing.setLocationId(incoming.getLocationId());

      merge(incoming.getLocalServerMappings(), existing.getLocalServerMappings(), LSC_COMPARATOR,
        existing::addLocalServerMapping, this::updateLscMapping, existing::removeLocalServerMapping);

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

}
