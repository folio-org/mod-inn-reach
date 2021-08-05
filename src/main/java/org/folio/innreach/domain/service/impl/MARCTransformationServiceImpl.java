package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.client.SourceRecordStorageClient;
import org.folio.innreach.domain.dto.folio.inventory.IdentifierWithConfigDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.dto.folio.inventory.SourceRecordType;
import org.folio.innreach.domain.dto.folio.inventory.SourceRecordDTO;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.exception.MarcRecordTransformationException;
import org.folio.innreach.domain.service.MARCTransformationOptionsSettingsService;
import org.folio.innreach.domain.service.MARCTransformationService;
import org.folio.innreach.dto.FieldConfigurationDTO;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsDTO;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Log4j2
@Service
public class MARCTransformationServiceImpl implements MARCTransformationService {

  public static final String MARC_FIELD_CODE_001 = "001";
  public static final String MARC_FIELD_CODE_9XX_PREFIX = "9";

  private final InventoryClient inventoryClient;
  private final SourceRecordStorageClient sourceRecordStorageClient;
  private final MARCTransformationOptionsSettingsService marcTransformationSettingsService;

  /*

    1. When provided with an Inventory Instance ID and a central server configuration ID, retrieve the instance record

    5. Retrieve transformation configuration settings from record contribution settings for the provided central server configuration ID (MODINREACH-50)
    6. Find relevant identifiers in the [index-]order specified in the retrieved configuration
    7. If the identifier value begins with a prefix value from the "ignore prefix" list for the identifier, discard the identifier
    8. Repeat step 7 until the first valid identifier or there are no more matching identifiers
    9. Update MARC 001 field to the first valid identifier value, stripping any alpha prefix (if indicated by match field options settings for the identifier type)
    10. Strip MARC fields or subfields indicated for exclusion in transformation configuration settings ("strip fields or subfields")
   */

  @Override
  public SourceRecordDTO transformRecord(UUID centralServerId, UUID inventoryId) {
    //1. When provided with an Inventory Instance ID and a central server configuration ID, retrieve the instance record
    var inventoryInstance = inventoryClient.getInstanceById(inventoryId);

    /*
      2. Determine if the record has an associate SRS MARC record
      3. Skip records without underlying SRS MARC. Return a message that indicates no underlying MARC available
      4. Retrieve underlying MARC record from SRS
     */
    var sourceRecord = getSourceRecord(inventoryId);

    //5. Retrieve transformation configuration settings from record contribution settings for the provided central server configuration ID (MODINREACH-50)
    var marcTransformationSettings = getMARCTransformationSettings(centralServerId);

    /*
      6. Find relevant identifiers in the [index-]order specified in the retrieved configuration
      7. If the identifier value begins with a prefix value from the "ignore prefix" list for the identifier, discard the identifier
      8. Repeat step 7 until the first valid identifier or there are no more matching identifiers
    */
    var validIdentifierWithConfig = findValidIdentifier(marcTransformationSettings, inventoryInstance);

    var excludedMARCFields = marcTransformationSettings.getExcludedMARCFields();
    var fieldsToDelete = new HashSet<SourceRecordDTO.RecordField>();

    /*
       9. Update MARC 001 field to the first valid identifier value, stripping any alpha prefix
       (if indicated by match field options settings for the identifier type)

       12. If no valid identifiers are found, leave the existing 001 value
     */
    validIdentifierWithConfig.ifPresent(identifier -> sourceRecord.getFields()
      .forEach(recordField -> {
        if (recordField.getCode().equals(MARC_FIELD_CODE_001)) {
          recordField.setValue(getIdentifierValue(identifier.getFieldConfigurationDTO(), identifier.getIdentifierDTO()));
        }
      })
    );

    // 10. Strip MARC fields or subfields indicated for exclusion in transformation configuration settings ("strip fields or subfields")
    sourceRecord.getFields().forEach(recordField -> {
      if (isFieldShouldBeDeleted(recordField, excludedMARCFields)) {
        fieldsToDelete.add(recordField);
      } else {
        recordField.getSubFields().removeAll(getSubFieldsToDelete(recordField, excludedMARCFields));
      }
    });

    sourceRecord.getFields().removeAll(fieldsToDelete);

    //todo - return transformed record
    return null;
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

  private String getIdentifierValue(FieldConfigurationDTO fieldConfiguration, InventoryInstanceDTO.IdentifierDTO identifier) {
      if (fieldConfiguration.getStripPrefix()) {
        return identifier.getValue(); //todo - add strip alpha prefix logic
      }
      return identifier.getValue();
    }

  private boolean isFieldShouldBeDeleted(SourceRecordDTO.RecordField recordField, List<String> excludedMARCFields) {
    // 11. Strip all 9xx MARC fields
    var recordFieldCode = recordField.getCode();
    return excludedMARCFields.contains(recordFieldCode) || recordFieldCode.startsWith(MARC_FIELD_CODE_9XX_PREFIX);
  }

  private List<SourceRecordDTO.RecordField> getSubFieldsToDelete(SourceRecordDTO.RecordField recordField,
                                                                 List<String> excludedMARCFields) {
    return recordField.getSubFields()
      .stream()
      .filter(subField -> excludedMARCFields.contains(subField.getCode()))
      .collect(Collectors.toList());
  }

}
