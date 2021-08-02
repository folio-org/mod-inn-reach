package org.folio.innreach.fixture;

import org.folio.innreach.domain.entity.FieldConfiguration;
import org.folio.innreach.domain.entity.MARCTransformationOptionsSettings;
import org.folio.innreach.dto.FieldConfigurationDTO;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsDTO;

import java.util.Random;
import java.util.UUID;

public class MARCTransformationOptionsSettingsFixture {

  public static MARCTransformationOptionsSettings createMARCTransformOptSet() {
    MARCTransformationOptionsSettings MARCTransformOptSet = new MARCTransformationOptionsSettings();
    MARCTransformOptSet.setConfigIsActive(new Random().nextBoolean());
    MARCTransformOptSet.getExcludedMARCFields().add("020");
    MARCTransformOptSet.getExcludedMARCFields().add("035");

    int fieldConfigsSize = new Random().nextInt(10) + 1;
    for (int i = 0; i < fieldConfigsSize; i++) {
      MARCTransformOptSet.getModifiedFieldsForContributedRecords().add(createFieldConfig());
    }

    return MARCTransformOptSet;
  }

  public static FieldConfiguration createFieldConfig() {
    FieldConfiguration fieldConfig = new FieldConfiguration();
    fieldConfig.setResourceIdentifierTypeId(UUID.randomUUID());
    fieldConfig.setStripPrefix(new Random().nextBoolean());
    fieldConfig.getIgnorePrefixes().add("LCCN");
    fieldConfig.getIgnorePrefixes().add("lccn");
    return fieldConfig;
  }

  public static MARCTransformationOptionsSettingsDTO createMARCTransformOptSetDTO() {
    MARCTransformationOptionsSettingsDTO MARCTransformOptSetDTO = new MARCTransformationOptionsSettingsDTO();
    MARCTransformOptSetDTO.setConfigIsActive(new Random().nextBoolean());
    MARCTransformOptSetDTO.addExcludedMARCFieldsItem("020");
    MARCTransformOptSetDTO.addExcludedMARCFieldsItem("035");

    int fieldConfigsSize = new Random().nextInt(10) + 1;
    for (int i = 0; i < fieldConfigsSize; i++) {
      MARCTransformOptSetDTO.addModifiedFieldsForContributedRecordsItem(createFieldConfigDTO());
    }

    return MARCTransformOptSetDTO;
  }

  public static FieldConfigurationDTO createFieldConfigDTO() {
    FieldConfigurationDTO fieldConfig = new FieldConfigurationDTO();
    fieldConfig.setResourceIdentifierTypeId(UUID.randomUUID());
    fieldConfig.setStripPrefix(new Random().nextBoolean());
    fieldConfig.addIgnorePrefixesItem("LCCN");
    fieldConfig.addIgnorePrefixesItem("lccn");
    return fieldConfig;
  }
}
