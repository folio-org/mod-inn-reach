package org.folio.innreach.domain.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.InnReachLocation;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.InnReachLocationService;
import org.folio.innreach.dto.InnReachLocationDTO;
import org.folio.innreach.dto.InnReachLocationsDTO;
import org.folio.innreach.mapper.InnReachLocationMapper;
import org.folio.innreach.repository.InnReachLocationRepository;

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

		return innReachLocationMapper.mapToInnReachLocationDTO(savedInnReachLocation);
	}

	@Override
	@Transactional(readOnly = true)
	public InnReachLocationDTO getInnReachLocation(UUID innReachLocationId) {
    return innReachLocationRepository.findById(innReachLocationId)
      .map(innReachLocationMapper::mapToInnReachLocationDTO)
      .orElseThrow(() -> new EntityNotFoundException("InnReachLocation with id: " + innReachLocationId + " not found!"));
	}

  @Override
  @Transactional(readOnly = true)
  public InnReachLocationsDTO getInnReachLocations(Iterable<UUID> innReachLocationIds) {
    var locations = innReachLocationRepository.findAllById(innReachLocationIds)
      .stream()
      .map(innReachLocationMapper::mapToInnReachLocationDTO)
      .collect(Collectors.toList());

    var locationsDTO = new InnReachLocationsDTO();
    locationsDTO.setLocations(locations);
    locationsDTO.setTotalRecords(locations.size());

    return locationsDTO;
  }

	@Override
	@Transactional(readOnly = true)
  public InnReachLocationsDTO getAllInnReachLocations(Integer offset, Integer limit) {
    var innReachLocations = collectInnReachLocations(offset, limit);

    var innReachLocationsDTO = new InnReachLocationsDTO();
    innReachLocationsDTO.setLocations(innReachLocations);
    innReachLocationsDTO.setTotalRecords(innReachLocations.size());

    return innReachLocationsDTO;
  }

  private List<InnReachLocationDTO> collectInnReachLocations(Integer offset, Integer limit){
    return innReachLocationRepository.findAll(PageRequest.of(offset, limit))
      .stream()
      .map(innReachLocationMapper::mapToInnReachLocationDTO)
      .collect(Collectors.toList());
  }

	@Override
	@Transactional
	public InnReachLocationDTO updateInnReachLocation(UUID innReachLocationId, InnReachLocationDTO innReachLocationDTO) {
    InnReachLocation innReachLocation = findInnReachLocationById(innReachLocationId);
    innReachLocation.setCode(innReachLocationDTO.getCode());
		innReachLocation.setDescription(innReachLocationDTO.getDescription());

		return innReachLocationMapper.mapToInnReachLocationDTO(innReachLocation);
	}

	@Override
	@Transactional
	public void deleteInnReachLocation(UUID innReachLocationId) {
    InnReachLocation innReachLocation = findInnReachLocationById(innReachLocationId);
    innReachLocationRepository.delete(innReachLocation);
	}

  private InnReachLocation findInnReachLocationById(UUID innReachLocationId) {
    return innReachLocationRepository.findById(innReachLocationId)
      .orElseThrow(() -> new EntityNotFoundException("InnReachLocation with id: " + innReachLocationId + " not found!"));
  }
}
