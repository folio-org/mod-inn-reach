package org.folio.innreach.fixture;


import javassist.bytecode.stackmap.TypeData;
import lombok.experimental.UtilityClass;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.folio.innreach.domain.entity.ContributionCriteriaExcludedLocation;
import org.folio.innreach.domain.entity.ContributionCriteriaStatisticalCodeBehavior;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@UtilityClass
public class ContributionCriteriaConfigurationFixture {
  public static final UUID CENTRAL_SERVER_UUID = UUID.fromString("91f89c86-2004-4f0e-807a-0c8247f293a6");

  public static final UUID USER_UUID_1 = UUID.fromString("de87fda1-9515-4e1f-85b6-45fc965415d1");
  public static final UUID USER_UUID_2 = UUID.fromString("de87fda1-9515-4e1f-85b6-45fc965415d2");

  public static final String[] EXCLUDED_LOCATION_IDs = {
    "efb089d4-4416-4892-ab81-bdfa00e4a2c3",
    "48a8dbb5-0a67-410f-b976-1b5cc7f97957",
    "f9ca8394-32ba-4fe5-825b-3a879fef65dd"
  };
  public static final String[] STATISTICAL_CODE_IDs = {
    "1d891eea-ef83-4952-93dc-9ca7fb9cb63e",
    "cc87119b-6c32-44b3-9c7d-24a7a3945edb",
    "db56b705-7c23-4fa1-9282-1a98b660c509"
  };


//  public static ContributionCriteriaConfigurationDTO createTestEntityDTO(LocalDateTime dateTime) {
//    MetaDataDTO metaDataDTO = MetaDataDTO.builder()
//      .createdDate(dateTime)
//      .createdByUserId(USER_UUID_1)
//      .updatedDate(dateTime)
//      .updatedByUserId(USER_UUID_1)
//      .build();
//
//    List<ContributionCriteriaExcludedLocationDTO> excludedLocationsDTOs = List.of(
//      ContributionCriteriaExcludedLocationDTO.builder()
//        .excludedLocationId(UUID.fromString(EXCLUDED_LOCATIONS[0]))
//        .build(),
//      ContributionCriteriaExcludedLocationDTO.builder()
//        .excludedLocationId(UUID.fromString(EXCLUDED_LOCATIONS[1]))
//        .build(),
//      ContributionCriteriaExcludedLocationDTO.builder()
//        .excludedLocationId(UUID.fromString(EXCLUDED_LOCATIONS[2]))
//        .build());
//
//    return ContributionCriteriaConfigurationDTO.builder()
//      .centralServeId(CENTRAL_SERVER_UUID)
//      .metaData(metaDataDTO)
//      .excludedLocations(excludedLocationsDTOs)
//      .build();
//  }

//  public static List<ContributionCriteriaConfigurationExcludedLocationDTO> createExcludedLocatioDTO() {
//  }


  public static ContributionCriteriaConfiguration createEntity(
    UUID id,
    List<ContributionCriteriaExcludedLocation> excludedLocations,
    List<ContributionCriteriaStatisticalCodeBehavior> statisticalCodeBehaviors) {
    var configuration = new ContributionCriteriaConfiguration();
    configuration.setCentralServeId(id);
//    configuration.setMetaData(metaData);

    configuration.setExcludedLocations(excludedLocations.stream()
      .collect(Collectors.toSet()));
    //ToDo: Stat code behaveior
    return configuration;
  }

  public static ContributionCriteriaConfiguration createTestContributionCriteriaConfiguration(UUID centralServerId) {
    ContributionCriteriaConfiguration configuration = new ContributionCriteriaConfiguration();
    configuration.setCentralServeId(centralServerId == null ? CENTRAL_SERVER_UUID : centralServerId);
    for (int i = 0; i < EXCLUDED_LOCATION_IDs.length; i++) {
      configuration.addExcludedLocationId(UUID.fromString(EXCLUDED_LOCATION_IDs[i]));
    }
    for (int i = 0; i < STATISTICAL_CODE_IDs.length; i++) {
      ContributionCriteriaStatisticalCodeBehavior statisticalCodeBehavior = new ContributionCriteriaStatisticalCodeBehavior();
      statisticalCodeBehavior.setContributionBehavior(ContributionCriteriaStatisticalCodeBehavior.ContributionBehavior.values()[i]);
      statisticalCodeBehavior.setStatisticalCodeId(UUID.fromString(STATISTICAL_CODE_IDs[i]));
      configuration.addStatisticalCodeBehavior(statisticalCodeBehavior);
    }
    return configuration;
  }

//  public static ContributionCriteriaConfigurationDTO createDTO(UUID crntralServerId) {
//
//    return ContributionCriteriaConfigurationDTO.builder()
//      .centralServerId(crntralServerId)
//
//      //ToDo: add metadata, excluded locations, statistical code behavior
//      .build();
//
//  }
}
