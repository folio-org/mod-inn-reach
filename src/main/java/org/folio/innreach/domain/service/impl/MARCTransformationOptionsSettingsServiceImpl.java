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
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityExistsException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;

@RequiredArgsConstructor
@Service
public class MARCTransformationOptionsSettingsServiceImpl implements MARCTransformationOptionsSettingsService {

  private final MARCTransformationOptionsSettingsRepository repository;
  private final MARCTransformationOptionsSettingsMapper mapper;

  private static final String TEXT_MARC_TRANSFORM_OPT_SET_WITH_ID = "MARC Transformation Options Settings with id: ";

  @Override
  public MARCTransformationOptionsSettingsDTO getMARCTransformOptSet(UUID centralServerId) {
    var marcTransformOptSet = findMARCTransformOptSet(centralServerId);
    return mapper.toMARCTransformationOptSetDto(marcTransformOptSet);
  }

  @Override
  public MARCTransformationOptionsSettingsDTO createMARCTransformOptSet(UUID centralServerId, MARCTransformationOptionsSettingsDTO marcTransformOptSetDTO) {
    repository.findById(centralServerId).ifPresent(marcTransformOptSet -> {
      throw new EntityExistsException(TEXT_MARC_TRANSFORM_OPT_SET_WITH_ID
        + marcTransformOptSet.getCentralServer().getId() + " already exists.");
    });
    var marcTransformOptSet = mapper.toMARCTransformationOptSet(marcTransformOptSetDTO);
    marcTransformOptSet.getModifiedFieldsForContributedRecords().forEach(modifiedField -> modifiedField.setMARCTransformationOptionsSettings(marcTransformOptSet));
    marcTransformOptSet.setCentralServer(centralServerRef(centralServerId));
    var createdMARCTransformOptSet = repository.save(marcTransformOptSet);
    return mapper.toMARCTransformationOptSetDto(createdMARCTransformOptSet);
  }

  @Override
  public MARCTransformationOptionsSettingsDTO updateMARCTransformOptSet(UUID centralServerId, MARCTransformationOptionsSettingsDTO marcTransformOptSetDTO) {
    var marcTransformOptSet = findMARCTransformOptSet(centralServerId);
    var updated = mapper.toMARCTransformationOptSet(marcTransformOptSetDTO);
    updateMARCTransformOptSet(marcTransformOptSet, updated);
    updateModifiedFields(marcTransformOptSet, updated);

    repository.save(marcTransformOptSet);

    return mapper.toMARCTransformationOptSetDto(marcTransformOptSet);
  }

  @Override
  @Transactional
  public void deleteMARCTransformOptSet(UUID centralServerId) {
    var marcTransformOptSet = findMARCTransformOptSet(centralServerId);
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

  private List<MARCTransformationOptionsSettingsDTO> collectMARCTransformOptSet(Integer offset, Integer limit){
    return repository.findAll(PageRequest.of(offset, limit))
      .stream()
      .map(mapper::toMARCTransformationOptSetDto)
      .collect(Collectors.toList());
  }

  private void updateMARCTransformOptSet(MARCTransformationOptionsSettings marcTransformOptSet, MARCTransformationOptionsSettings updated) {
    marcTransformOptSet.setExcludedMARCFields(updated.getExcludedMARCFields());
    marcTransformOptSet.setConfigIsActive(updated.getConfigIsActive());
  }

  private void updateModifiedFields(MARCTransformationOptionsSettings marcTransformOptSet, MARCTransformationOptionsSettings updated) {
    var currentModifiedFields = new ArrayList<>(marcTransformOptSet.getModifiedFieldsForContributedRecords());
    var updatedModifiedFields = updated.getModifiedFieldsForContributedRecords();

    // 1. Remove the existing database records that are no longer found in the incoming collection.
    var modifiedFieldsToDelete = new ArrayList<>(currentModifiedFields);
    modifiedFieldsToDelete.removeAll(updatedModifiedFields);
    modifiedFieldsToDelete.forEach(marcTransformOptSet::removeModifiedFieldForContributedRecords);

    // 2. Add the records found in the incoming collection, which cannot be found in the current database snapshot.
    var newModifiedFields = new ArrayList<>(updatedModifiedFields);
    newModifiedFields.removeAll(currentModifiedFields);
    newModifiedFields.forEach(marcTransformOptSet::addModifiedFieldForContributedRecords);

    // 3. Update the existing database records which can be found in the incoming collection.
    updatedModifiedFields.removeAll(newModifiedFields);
    for (FieldConfiguration updatedFieldConfiguration : updatedModifiedFields) {
      updatedFieldConfiguration.setMARCTransformationOptionsSettings(marcTransformOptSet);
      marcTransformOptSet.getModifiedFieldsForContributedRecords()
        .set(marcTransformOptSet.getModifiedFieldsForContributedRecords().indexOf(updatedFieldConfiguration), updatedFieldConfiguration);
    }
  }

  private MARCTransformationOptionsSettings findMARCTransformOptSet(UUID centralServerId) {
    return repository.findOne(exampleWithServerId(centralServerId))
      .orElseThrow(() -> new EntityNotFoundException("MARC Transformation Options Settings not found: " +
        "centralServerId = " + centralServerId));
  }

  private static Example<MARCTransformationOptionsSettings> exampleWithServerId(UUID centralServerId) {
    var toFind = new MARCTransformationOptionsSettings();
    toFind.setCentralServer(centralServerRef(centralServerId));

    return Example.of(toFind);
  }
}
