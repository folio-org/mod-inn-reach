package org.folio.innreach.fixture;

import org.folio.innreach.domain.entity.FieldConfiguration;
import org.folio.innreach.domain.entity.MARCTransformationOptionsSettings;

import java.util.Random;
import java.util.UUID;

public class MARCTransformationOptionsSettingsFixture {

  public static MARCTransformationOptionsSettings createMARCTransformOptSet(){
    MARCTransformationOptionsSettings MARCTransformOptSet = new MARCTransformationOptionsSettings();
    MARCTransformOptSet.setConfigIsActive(new Random().nextBoolean());
    MARCTransformOptSet.getExcludedMARCFields().add("020");
    MARCTransformOptSet.getExcludedMARCFields().add("035");

    int fieldConfigsSize = new Random().nextInt(10);
    for (int i = 0; i < fieldConfigsSize; i++){
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
}
