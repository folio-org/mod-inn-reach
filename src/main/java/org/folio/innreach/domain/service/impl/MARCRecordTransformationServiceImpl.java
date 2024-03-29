package org.folio.innreach.domain.service.impl;

import static java.lang.Boolean.TRUE;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.client.SourceRecordStorageClient;
import org.folio.innreach.converter.marc.TransformedMARCRecordConverter;
import org.folio.innreach.domain.dto.folio.inventory.IdentifierWithConfigDTO;
import org.folio.innreach.domain.dto.folio.sourcerecord.ParsedRecordDTO;
import org.folio.innreach.domain.dto.folio.sourcerecord.RecordFieldDTO;
import org.folio.innreach.domain.dto.folio.sourcerecord.SourceRecordDTO;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.exception.MarcRecordTransformationException;
import org.folio.innreach.domain.service.MARCRecordTransformationService;
import org.folio.innreach.domain.service.MARCTransformationOptionsSettingsService;
import org.folio.innreach.dto.FieldConfigurationDTO;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.InstanceIdentifiers;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsDTO;
import org.folio.innreach.dto.TransformedMARCRecordDTO;

@RequiredArgsConstructor
@Log4j2
@Service
public class MARCRecordTransformationServiceImpl implements MARCRecordTransformationService {

  private static final String MARC_RECORD_SOURCE = "MARC";
  private static final String MARC_FIELD_CODE_001 = "001";
  private static final String MARC_FIELD_CODE_9XX_PREFIX = "9";
  private static final String NOT_NUMBERS_REGEXP = "\\D+";

  private final InstanceStorageClient instanceStorageClient;
  private final SourceRecordStorageClient sourceRecordStorageClient;
  private final MARCTransformationOptionsSettingsService marcTransformationSettingsService;
  private final TransformedMARCRecordConverter transformedMarcRecordConverter;

  @Override
  public TransformedMARCRecordDTO transformRecord(UUID centralServerId, UUID inventoryId) {
    var inventoryInstance = instanceStorageClient.getInstanceById(inventoryId);

    return transformRecord(centralServerId, inventoryInstance);
  }

  @Override
  public TransformedMARCRecordDTO transformRecord(UUID centralServerId, Instance inventoryInstance) {
    log.debug("transformRecord:: parameters centralServerId: {}, inventoryInstance: {}", centralServerId, inventoryInstance);
    if (!isMARCRecord(inventoryInstance)) {
      throw new MarcRecordTransformationException(
        String.format("Source [%s] of inventory instance with id [%s] is not MARC", inventoryInstance.getSource(), inventoryInstance.getId())
      );
    }

    var sourceRecord = getSourceRecord(inventoryInstance.getId());

    var marcTransformationSettings = getMARCTransformationSettings(centralServerId);

    var validIdentifierWithConfig = findValidIdentifier(marcTransformationSettings, inventoryInstance);

    var excludedMARCFields = marcTransformationSettings.getExcludedMARCFields();

    var parsedRecord = sourceRecord.getParsedRecord();

    var fieldsToDelete = new HashSet<RecordFieldDTO>();

    validIdentifierWithConfig.ifPresent(identifierWithConf -> updateRecord001MARCField(parsedRecord, identifierWithConf));

    parsedRecord.getFields().forEach(recordField -> {
      if (isFieldShouldBeDeleted(recordField, excludedMARCFields)) {
        fieldsToDelete.add(recordField);
      } else if (recordField.getSubFields() != null) {
        recordField.getSubFields().removeAll(getSubFieldsToDelete(recordField, excludedMARCFields));
      }
    });

    parsedRecord.getFields().removeAll(fieldsToDelete);

    return transformedMarcRecordConverter.toTransformedRecord(sourceRecord);
  }

  public static boolean isMARCRecord(Instance inventoryInstance) {
    return MARC_RECORD_SOURCE.equalsIgnoreCase(inventoryInstance.getSource());
  }

  private SourceRecordDTO getSourceRecord(UUID instanceId) {
    return sourceRecordStorageClient.getRecordByInstanceId(instanceId);
  }

  private MARCTransformationOptionsSettingsDTO getMARCTransformationSettings(UUID centralServerId) {
    log.debug("getMARCTransformationSettings:: parameters centralServerId: {}", centralServerId);
    MARCTransformationOptionsSettingsDTO transformationConfig;
    try {
      transformationConfig = marcTransformationSettingsService.get(centralServerId);
    } catch (EntityNotFoundException e) {
      log.warn("MARC transformation settings for CentralServer Id [{}] is not found. Using default", centralServerId);
      transformationConfig = createEmptyMARCTransformationConfig();
    }

    log.info("getMARCTransformationSettings:: result: {}", transformationConfig);
    return transformationConfig;
  }

  private Optional<IdentifierWithConfigDTO> findValidIdentifier(MARCTransformationOptionsSettingsDTO settings,
                                                                Instance inventoryInstance) {
    if (!TRUE.equals(settings.getConfigIsActive())) {
      return Optional.empty();
    }

    return inventoryInstance.getIdentifiers()
      .stream()
      .map(identifier -> findFirstValidIdentifierWithConfig(settings, identifier))
      .filter(Objects::nonNull)
      .findFirst();
  }

  private IdentifierWithConfigDTO findFirstValidIdentifierWithConfig(MARCTransformationOptionsSettingsDTO settings,
                                                                     InstanceIdentifiers identifier) {
    return settings.getModifiedFieldsForContributedRecords()
      .stream()
      .filter(fieldConfig -> Objects.equals(fieldConfig.getResourceIdentifierTypeId(), identifier.getIdentifierTypeId()))
      .filter(fieldConfig -> !identifierValueStartsWithIgnorePrefix(fieldConfig, identifier))
      .map(fieldConfig -> new IdentifierWithConfigDTO(identifier, fieldConfig))
      .findFirst()
      .orElse(null);
  }

  private boolean identifierValueStartsWithIgnorePrefix(FieldConfigurationDTO fieldConfiguration,
                                                        InstanceIdentifiers identifier) {
    return fieldConfiguration.getIgnorePrefixes()
      .stream()
      .anyMatch(ignorePrefix -> identifier.getValue().startsWith(ignorePrefix));
  }

  private String stripAlphaPrefix(FieldConfigurationDTO fieldConfiguration,
                                  InstanceIdentifiers identifier) {
    if (fieldConfiguration.getStripPrefix()) {
      return identifier.getValue().replaceAll(NOT_NUMBERS_REGEXP, "");
    }
    return identifier.getValue();
  }

  private boolean isFieldShouldBeDeleted(RecordFieldDTO recordField, List<String> excludedMARCFields) {
    var recordFieldCode = recordField.getCode();
    return excludedMARCFields.contains(recordFieldCode) || recordFieldCode.startsWith(MARC_FIELD_CODE_9XX_PREFIX);
  }

  private List<RecordFieldDTO.SubFieldDTO> getSubFieldsToDelete(RecordFieldDTO recordField, List<String> excludedMARCFields) {
    return recordField.getSubFields()
      .stream()
      .filter(subField -> excludedMARCFields.contains(subField.getCode().toString()))
      .collect(Collectors.toList());
  }

  private void updateRecord001MARCField(ParsedRecordDTO parsedRecord, IdentifierWithConfigDTO identifierWithConfig) {
    parsedRecord.getFields().forEach(recordField -> {
      if (MARC_FIELD_CODE_001.equals(recordField.getCode())) {
        var fieldConfig = identifierWithConfig.getFieldConfigurationDTO();
        var identifier = identifierWithConfig.getIdentifierDTO();

        recordField.setValue(stripAlphaPrefix(fieldConfig, identifier));
      }
    });
  }

  private static MARCTransformationOptionsSettingsDTO createEmptyMARCTransformationConfig() {
    var config = new MARCTransformationOptionsSettingsDTO();
    config.setConfigIsActive(true);
    config.setExcludedMARCFields(Collections.emptyList());
    config.setModifiedFieldsForContributedRecords(Collections.emptyList());
    return config;
  }

}
