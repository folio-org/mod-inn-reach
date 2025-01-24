package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.merge;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.AgencyLocationAcMapping;
import org.folio.innreach.domain.entity.AgencyLocationLscMapping;
import org.folio.innreach.domain.entity.AgencyLocationMapping;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.AgencyMappingService;
import org.folio.innreach.domain.service.CentralServerConfigurationService;
import org.folio.innreach.dto.AgencyLocationLscMappingDTO;
import org.folio.innreach.dto.AgencyLocationMappingDTO;
import org.folio.innreach.dto.LocalServer;
import org.folio.innreach.mapper.AgencyLocationMappingMapper;
import org.folio.innreach.repository.AgencyLocationMappingRepository;

@Log4j2
@RequiredArgsConstructor
@Service
@Transactional
public class AgencyMappingServiceImpl implements AgencyMappingService {

  private static final String NOT_FOUND_MESSAGE_FMT = "Agency mapping with central server id: %s not found";
  private static final Comparator<AgencyLocationLscMapping> LSC_COMPARATOR = Comparator.comparing(AgencyLocationLscMapping::getLocalServerCode);
  private static final Comparator<AgencyLocationAcMapping> AC_COMPARATOR = Comparator.comparing(AgencyLocationAcMapping::getAgencyCode);

  private final AgencyLocationMappingRepository repository;
  private final AgencyLocationMappingMapper mapper;
  private final CentralServerConfigurationService configurationService;

  @Override
  public AgencyLocationMappingDTO getMapping(UUID centralServerId) {
    log.debug("getMapping:: parameters centralServerId: {}", centralServerId);
    return fetchOne(centralServerId)
      .map(mapper::toDTO)
      .orElseThrow(() -> new EntityNotFoundException(String.format(NOT_FOUND_MESSAGE_FMT, centralServerId)));
  }

  @Override
  public AgencyLocationMappingDTO updateMapping(UUID centralServerId, AgencyLocationMappingDTO mappingDto) {
    log.debug("updateMapping:: parameters centralServerId: {}, mappingDTO: {}", centralServerId, mappingDto);
    var incoming = mapper.toEntityWithRefs(mappingDto, centralServerId);

    var updated = fetchOne(centralServerId)
      .map(mergeFunc(incoming))
      .orElse(incoming);

    repository.saveAndFlush(updated);

    log.info("updateMapping:: result: {}", mapper.toDTO(updated));
    return mapper.toDTO(updated);
  }

  @Override
  public UUID getLocationIdByAgencyCode(UUID centralServerId, String agencyCode) {
    log.debug("getLocationIdByAgencyCode:: parameters centralServerId: {}, agencyCode: {}", centralServerId, agencyCode);
    var mapping = getMapping(centralServerId);

    log.info("getLocationIdByAgencyCode:: Returned locationId by agency code: {}", agencyCode);
    return getLocationIdByAgencyCode(mapping, agencyCode)
      .or(() -> getLocationIdByLocalServer(mapping, agencyCode, centralServerId))
      .orElse(mapping.getLocationId());
  }

  private LocalServer getLocalServerByAgencyCode(UUID centralServerId, String agencyCode) {
    log.debug("getLocalServerByAgencyCode:: parameters centralServerId: {}, agencyCode: {}", centralServerId, agencyCode);
    var localServers = configurationService.getLocalServers(centralServerId);

    for (var localServer : localServers) {
      var agencies = localServer.getAgencyList();
      var agency = agencies.stream().filter(a -> agencyCode.equals(a.getAgencyCode())).findFirst();

      if (agency.isPresent()) {
        log.info("getLocalServerByAgencyCode:: result: {}", localServer);
        return localServer;
      }
    }

    throw new IllegalArgumentException("Central agency for code " + agencyCode + " is not found");
  }

  private Optional<UUID> getLocationIdByLocalServer(AgencyLocationMappingDTO mapping, String agencyCode, UUID centralServerId) {
    log.debug("getLocationIdByLocalServer:: parameters mapping: {}, agencyCode: {}, centralServerId: {}", mapping, agencyCode, centralServerId);
    var localServer = getLocalServerByAgencyCode(centralServerId, agencyCode);
    var localCode = localServer.getLocalCode();

    return mapping.getLocalServers().stream()
      .filter(server -> localCode.equals(server.getLocalCode()))
      .map(AgencyLocationLscMappingDTO::getLocationId)
      .map(locationId -> {
        if (locationId == null) {
          log.warn("getLocationIdByLocalServer:: locationId is null in localServer with localCode: {}", localCode);
        }
        return locationId;
      })
      .filter(Objects::nonNull)
      .findFirst();
  }

  private Optional<UUID> getLocationIdByAgencyCode(AgencyLocationMappingDTO mapping, String agencyCode) {
    log.info("getLocationIdByAgencyCode:: Start - parameters mapping: {}, agencyCode: {}", mapping, agencyCode);

    return mapping.getLocalServers().stream()
      .map(AgencyLocationLscMappingDTO::getAgencyCodeMappings)
      .flatMap(Collection::stream)
      .filter(m -> agencyCode.equals(m.getAgencyCode()))
      .map(mappingEntry -> {
        UUID locationId = mappingEntry.getLocationId();
        if (locationId == null) {
          log.warn("getLocationIdByAgencyCode:: locationId is null in mapping: {}", mappingEntry);
        }
        return locationId;
      })
      .filter(Objects::nonNull)
      .findFirst();
  }


  private Optional<AgencyLocationMapping> fetchOne(UUID centralServerId) {
    log.debug("fetchOne:: parameters centralServerId: {}", centralServerId);
    return repository.fetchOneByCsId(centralServerId);
  }

  private Function<AgencyLocationMapping, AgencyLocationMapping> mergeFunc(AgencyLocationMapping incoming) {
    log.debug("mergeFunc:: parameters incoming: {}", incoming);
    return existing -> {
      existing.setLibraryId(incoming.getLibraryId());
      existing.setLocationId(incoming.getLocationId());

      merge(incoming.getLocalServerMappings(), existing.getLocalServerMappings(), LSC_COMPARATOR,
        existing::addLocalServerMapping, this::updateLscMapping, existing::removeLocalServerMapping);

      return existing;
    };
  }

  private void updateLscMapping(AgencyLocationLscMapping incoming, AgencyLocationLscMapping existing) {
    log.debug("updateLscMapping:: parameters incoming: {}, existing: {}", incoming, existing);
    existing.setLocationId(incoming.getLocationId());
    existing.setLibraryId(incoming.getLibraryId());

    Set<AgencyLocationAcMapping> agencyCodeMappings = incoming.getAgencyCodeMappings();
    Set<AgencyLocationAcMapping> existingAgencyCodeMappings = existing.getAgencyCodeMappings();

    merge(agencyCodeMappings, existingAgencyCodeMappings, AC_COMPARATOR,
      existing::addAgencyCodeMapping, this::updateAcMapping, existing::removeAgencyCodeMapping);
    log.info("updateLscMapping:: Lsc mapping updated");
  }

  private void updateAcMapping(AgencyLocationAcMapping incoming, AgencyLocationAcMapping existing) {
    existing.setLocationId(incoming.getLocationId());
    existing.setLibraryId(incoming.getLibraryId());
  }

}
