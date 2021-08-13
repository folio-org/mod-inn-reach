package org.folio.innreach.domain.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.client.SourceRecordStorageClient;
import org.folio.innreach.converter.marc.TransformedMARCRecordConverter;
import org.folio.innreach.domain.dto.folio.inventory.IdentifierWithConfigDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.dto.folio.sourcerecord.ParsedRecordDTO;
import org.folio.innreach.domain.dto.folio.sourcerecord.RecordFieldDTO;
import org.folio.innreach.domain.dto.folio.sourcerecord.SourceRecordDTO;
import org.folio.innreach.domain.dto.folio.sourcerecord.SourceRecordType;
import org.folio.innreach.domain.exception.MarcRecordTransformationException;
import org.folio.innreach.domain.service.MARCRecordTransformationService;
import org.folio.innreach.domain.service.MARCTransformationOptionsSettingsService;
import org.folio.innreach.dto.FieldConfigurationDTO;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsDTO;
import org.folio.innreach.dto.TransformedMARCRecordDTO;

@RequiredArgsConstructor
@Log4j2
@Service
public class MARCRecordTransformationServiceImpl implements MARCRecordTransformationService {

  private static final String MARC_FIELD_CODE_001 = "001";
  private static final String MARC_FIELD_CODE_9XX_PREFIX = "9";
  private static final String NOT_NUMBERS_REGEXP = "\\D+";

  private final InventoryClient inventoryClient;
  private final SourceRecordStorageClient sourceRecordStorageClient;
  private final MARCTransformationOptionsSettingsService marcTransformationSettingsService;
  private final TransformedMARCRecordConverter transformedMarcRecordConverter;

  @Override
  public TransformedMARCRecordDTO transformRecord(UUID centralServerId, UUID inventoryId) {
    var inventoryInstance = inventoryClient.getInstanceById(inventoryId);

    var sourceRecord = getSourceRecord(inventoryId);

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

  private SourceRecordDTO getSourceRecord(UUID inventoryId) {
    var sourceRecord = sourceRecordStorageClient.getRecordById(inventoryId);

    if (!isMARCRecord(sourceRecord)) {
      throw new MarcRecordTransformationException(
        String.format("SourceRecord with Id [%s] is not MARC", sourceRecord.getId())
      );
    }

    return sourceRecord;
  }

  private boolean isMARCRecord(SourceRecordDTO sourceRecord) {
    return sourceRecord.getRecordType().equals(SourceRecordType.MARC.name());
  }

  private MARCTransformationOptionsSettingsDTO getMARCTransformationSettings(UUID centralServerId) {
    var marcTransformationSettings = marcTransformationSettingsService.get(centralServerId);

    if (!marcTransformationSettings.getConfigIsActive()) {
      throw new IllegalStateException(
        String.format("MARC transformation settings for CentralServer Id [%s] is not active", centralServerId)
      );
    }

    return marcTransformationSettings;
  }

  private Optional<IdentifierWithConfigDTO> findValidIdentifier(MARCTransformationOptionsSettingsDTO marcTransformationSettings,
                                                                InventoryInstanceDTO inventoryInstance) {
    return inventoryInstance.getIdentifiers()
      .stream()
      .map(identifier -> findFirstValidIdentifierWithConfig(marcTransformationSettings, identifier))
      .filter(Objects::nonNull)
      .findFirst();
  }

  private IdentifierWithConfigDTO findFirstValidIdentifierWithConfig(MARCTransformationOptionsSettingsDTO marcTransformationSettings,
                                                                     InventoryInstanceDTO.IdentifierDTO identifier) {
    return marcTransformationSettings.getModifiedFieldsForContributedRecords()
      .stream()
      .filter(fieldConfig -> fieldConfig.getResourceIdentifierTypeId().equals(identifier.getId()))
      .filter(fieldConfig -> !identifierValueStartsWithIgnorePrefix(fieldConfig, identifier))
      .map(fieldConfig -> new IdentifierWithConfigDTO(identifier, fieldConfig))
      .findFirst()
      .orElse(null);
  }

  private boolean identifierValueStartsWithIgnorePrefix(FieldConfigurationDTO fieldConfiguration,
                                                        InventoryInstanceDTO.IdentifierDTO identifier) {
    return fieldConfiguration.getIgnorePrefixes()
      .stream()
      .anyMatch(ignorePrefix -> identifier.getValue().startsWith(ignorePrefix));
  }

  private String stripIdentifierValueAlphaPrefix(FieldConfigurationDTO fieldConfiguration,
                                                 InventoryInstanceDTO.IdentifierDTO identifier) {
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
      .filter(subField -> excludedMARCFields.contains(subField.getCode()))
      .collect(Collectors.toList());
  }

  private void updateRecord001MARCField(ParsedRecordDTO parsedRecord, IdentifierWithConfigDTO identifierWithConfig) {
    parsedRecord.getFields().forEach(recordField -> {
      if (recordField.getCode().equals(MARC_FIELD_CODE_001)) {
        recordField.setValue(stripIdentifierValueAlphaPrefix(identifierWithConfig.getFieldConfigurationDTO(),
          identifierWithConfig.getIdentifierDTO()));
      }
    });
  }

}
