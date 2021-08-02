package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.entity.FieldConfiguration;
import org.folio.innreach.domain.entity.MARCTransformationOptionsSettings;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.MARCTransformationOptionsSettingsService;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsListDTO;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsDTO;
import org.folio.innreach.mapper.MARCTransformationOptionsSettingsMapper;
import org.folio.innreach.repository.MARCTransformationOptionsSettingsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityExistsException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;

@RequiredArgsConstructor
@Service
public class MARCTransformationOptionsSettingsServiceImpl implements MARCTransformationOptionsSettingsService {

  private final MARCTransformationOptionsSettingsRepository repository;
  private final MARCTransformationOptionsSettingsMapper mapper;

  private static final String TEXT_MARC_TRANSFORM_OPT_SET_NOT_FOUND = "MARC Transformation Options Settings not found: centralServerId =";
  private static final Comparator<FieldConfiguration> comparator = Comparator.comparing(FieldConfiguration::getResourceIdentifierTypeId);

  @Override
  public MARCTransformationOptionsSettingsDTO get(UUID centralServerId) {
    var marcTransformOptSet = repository.findOneByCentralServerId(centralServerId);
    return marcTransformOptSet.map(mapper::toDto).orElseThrow(()
      -> new EntityNotFoundException(TEXT_MARC_TRANSFORM_OPT_SET_NOT_FOUND + centralServerId));
  }

  @Override
  public MARCTransformationOptionsSettingsDTO create(UUID centralServerId, MARCTransformationOptionsSettingsDTO marcTransformOptSetDTO) {
    repository.findOneByCentralServerId(centralServerId).ifPresent(m -> {
      throw new EntityExistsException("MARC Transformation Options Settings for centralServerId = " + centralServerId
        + "already exists.");
    });

    var marcTransformOptSet = mapper.toEntity(marcTransformOptSetDTO);
    marcTransformOptSet.getModifiedFieldsForContributedRecords().forEach(modifiedField -> modifiedField.setMARCTransformationOptionsSettings(marcTransformOptSet));
    marcTransformOptSet.setCentralServer(centralServerRef(centralServerId));
    var createdMARCTransformOptSet = repository.save(marcTransformOptSet);
    return mapper.toDto(createdMARCTransformOptSet);
  }

  @Override
  public MARCTransformationOptionsSettingsDTO update(UUID centralServerId, MARCTransformationOptionsSettingsDTO marcTransformOptSetDTO) {
    var marcTransformOptSet = repository.findOneByCentralServerId(centralServerId).orElseThrow(
      () -> new EntityNotFoundException(TEXT_MARC_TRANSFORM_OPT_SET_NOT_FOUND + centralServerId));

    var updated = mapper.toEntity(marcTransformOptSetDTO);
    updateMARCTransformOptSet(marcTransformOptSet, updated);
    updateModifiedFields(marcTransformOptSet, updated);

    repository.save(marcTransformOptSet);

    return mapper.toDto(marcTransformOptSet);
  }

  @Override
  @Transactional
  public void delete(UUID centralServerId) {
    var marcTransformOptSet = repository.findOneByCentralServerId(centralServerId).orElseThrow(
      () -> new EntityNotFoundException(TEXT_MARC_TRANSFORM_OPT_SET_NOT_FOUND + centralServerId));
    repository.delete(marcTransformOptSet);
  }

  @Override
  public MARCTransformationOptionsSettingsListDTO getAll(int offset, int limit) {
    var marcTransformOptSetList = collectMARCTransformOptSet(offset, limit);

    var marcTransformOptSetListDTO = new MARCTransformationOptionsSettingsListDTO();
    marcTransformOptSetListDTO.setMaRCTransformOptSetList(marcTransformOptSetList);
    marcTransformOptSetListDTO.setTotalRecords(marcTransformOptSetList.size());

    return marcTransformOptSetListDTO;
  }

  private List<MARCTransformationOptionsSettingsDTO> collectMARCTransformOptSet(Integer offset, Integer limit) {
    return repository.findAll(PageRequest.of(offset, limit))
      .stream()
      .map(mapper::toDto)
      .collect(Collectors.toList());
  }

  private void updateMARCTransformOptSet(MARCTransformationOptionsSettings marcTransformOptSet, MARCTransformationOptionsSettings updated) {
    marcTransformOptSet.setExcludedMARCFields(updated.getExcludedMARCFields());
    marcTransformOptSet.setConfigIsActive(updated.getConfigIsActive());
  }

  private void updateModifiedFields(MARCTransformationOptionsSettings marcTransformOptSet, MARCTransformationOptionsSettings updated) {
    ServiceUtils.merge(updated.getModifiedFieldsForContributedRecords(), marcTransformOptSet.getModifiedFieldsForContributedRecords(), comparator, marcTransformOptSet::addModifiedFieldForContributedRecords,
      this::updateModifiedFieldData, marcTransformOptSet::removeModifiedFieldForContributedRecords);
  }

  private void updateModifiedFieldData(FieldConfiguration incoming, FieldConfiguration existing) {
    existing.setStripPrefix(incoming.getStripPrefix());
    existing.setIgnorePrefixes(incoming.getIgnorePrefixes());
  }

}
