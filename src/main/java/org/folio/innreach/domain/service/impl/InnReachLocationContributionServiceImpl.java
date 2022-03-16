package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Example;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.entity.InnReachLocation;
import org.folio.innreach.domain.entity.LibraryMapping;
import org.folio.innreach.domain.entity.LocationMapping;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.InnReachLocationContributionService;
import org.folio.innreach.external.dto.InnReachLocationDTO;
import org.folio.innreach.external.service.InnReachLocationExternalService;
import org.folio.innreach.repository.LibraryMappingRepository;
import org.folio.innreach.repository.LocationMappingRepository;

@Log4j2
@RequiredArgsConstructor
@Service
public class InnReachLocationContributionServiceImpl implements InnReachLocationContributionService {

  private final LocationMappingRepository locationMappingRepository;
  private final LibraryMappingRepository libraryMappingRepository;
  private final CentralServerService centralServerService;
  private final InnReachLocationExternalService innReachLocationExternalService;

  @Async
  @Override
  public void contributeInnReachLocations(UUID centralServerId) {
    var mappedLocations = getCentralServerMappedLocations(centralServerId);

    var centralServerConnectionDetails = centralServerService.getCentralServerConnectionDetails(centralServerId);

    innReachLocationExternalService.submitMappedLocationsToInnReach(centralServerConnectionDetails, mappedLocations);
  }

  private List<InnReachLocationDTO> getCentralServerMappedLocations(UUID centralServerId) {
    var locationMappings = fetchLocationMappings(centralServerId);
    var libraryMappings = fetchLibraryMappings(centralServerId);

    return Stream.concat(
        locationMappings.stream().map(LocationMapping::getInnReachLocation),
        libraryMappings.stream().map(LibraryMapping::getInnReachLocation))
      .map(this::toInnReachLocationDTO)
      .distinct()
      .collect(Collectors.toList());
  }

  private InnReachLocationDTO toInnReachLocationDTO(InnReachLocation innReachLocation) {
    return new InnReachLocationDTO(innReachLocation.getCode(), innReachLocation.getDescription());
  }

  private List<LocationMapping> fetchLocationMappings(UUID centralServerId) {
    return locationMappingRepository.findByCentralServerId(centralServerId);
  }

  private List<LibraryMapping> fetchLibraryMappings(UUID centralServerId) {
    return libraryMappingRepository.findAll(libMappingExampleWithServerId(centralServerId));
  }

  private static Example<LibraryMapping> libMappingExampleWithServerId(UUID centralServerId) {
    var toFind = new LibraryMapping();
    toFind.setCentralServer(centralServerRef(centralServerId));

    return Example.of(toFind);
  }

}
