package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.util.ListUtils.mapItems;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.InnReachLocation;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.InnReachLocationService;
import org.folio.innreach.dto.InnReachLocationDTO;
import org.folio.innreach.dto.InnReachLocationsDTO;
import org.folio.innreach.mapper.InnReachLocationMapper;
import org.folio.innreach.repository.InnReachLocationRepository;
import org.folio.spring.data.OffsetRequest;

@Log4j2
@RequiredArgsConstructor
@Service
public class InnReachLocationServiceImpl implements InnReachLocationService {

	private final InnReachLocationRepository innReachLocationRepository;
	private final InnReachLocationMapper innReachLocationMapper;

	@Override
	@Transactional
	public InnReachLocationDTO createInnReachLocation(InnReachLocationDTO innReachLocationDTO) {
		var innReachLocation = innReachLocationMapper.mapToInnReachLocation(innReachLocationDTO);

		var savedInnReachLocation = innReachLocationRepository.save(innReachLocation);

    log.info("createInnReachLocation:: created Inn Reach Location: [{}]", savedInnReachLocation.getId());
		return innReachLocationMapper.mapToInnReachLocationDTO(savedInnReachLocation);
	}

	@Override
	@Transactional(readOnly = true)
	public InnReachLocationDTO getInnReachLocation(UUID innReachLocationId) {
    log.debug("getInnReachLocation:: parameters innReachLocationId: {}", innReachLocationId);
    return innReachLocationRepository.findById(innReachLocationId)
      .map(innReachLocationMapper::mapToInnReachLocationDTO)
      .orElseThrow(() -> new EntityNotFoundException("InnReachLocation with id: " + innReachLocationId + " not found!"));
	}

  @Override
  @Transactional(readOnly = true)
  public InnReachLocationsDTO getInnReachLocations(Iterable<UUID> innReachLocationIds) {
    log.debug("getInnReachLocations:: parameters innReachLocationIds: {}", innReachLocationIds);
    var locations = mapItems(innReachLocationRepository.findAllById(innReachLocationIds),
        innReachLocationMapper::mapToInnReachLocationDTO);

    var locationsDTO = new InnReachLocationsDTO();
    locationsDTO.setLocations(locations);
    locationsDTO.setTotalRecords(locations.size());

    return locationsDTO;
  }

	@Override
	@Transactional(readOnly = true)
  public InnReachLocationsDTO getAllInnReachLocations(Integer offset, Integer limit) {
    log.debug("getAllInnReachLocations:: parameters offset: {}, limit: {}", offset, limit);
    var innReachLocations = collectInnReachLocations(offset, limit);

    var innReachLocationsDTO = new InnReachLocationsDTO();
    innReachLocationsDTO.setLocations(innReachLocations);
    innReachLocationsDTO.setTotalRecords(innReachLocations.size());

    return innReachLocationsDTO;
  }

  private List<InnReachLocationDTO> collectInnReachLocations(Integer offset, Integer limit){
    return innReachLocationRepository.findAll(new OffsetRequest(offset, limit))
      .stream()
      .map(innReachLocationMapper::mapToInnReachLocationDTO)
      .toList();
  }

	@Override
	@Transactional
	public InnReachLocationDTO updateInnReachLocation(UUID innReachLocationId, InnReachLocationDTO innReachLocationDTO) {
    InnReachLocation innReachLocation = findInnReachLocationById(innReachLocationId);
    innReachLocation.setCode(innReachLocationDTO.getCode());
		innReachLocation.setDescription(innReachLocationDTO.getDescription());

    log.info("updateInnReachLocation:: Updated Inn Reach Location: [{}]", innReachLocationId);
		return innReachLocationMapper.mapToInnReachLocationDTO(innReachLocation);
	}

	@Override
	@Transactional
	public void deleteInnReachLocation(UUID innReachLocationId) {
    log.debug("deleteInnReachLocation:: parameters innReachLocationId: {}", innReachLocationId);
    InnReachLocation innReachLocation = findInnReachLocationById(innReachLocationId);
    innReachLocationRepository.delete(innReachLocation);
    log.info("deleteInnReachLocation:: InnReach location deleted: [{}]", innReachLocationId);
	}

  private InnReachLocation findInnReachLocationById(UUID innReachLocationId) {
    return innReachLocationRepository.findById(innReachLocationId)
      .orElseThrow(() -> new EntityNotFoundException("InnReachLocation with id: " + innReachLocationId + " not found!"));
  }
}
