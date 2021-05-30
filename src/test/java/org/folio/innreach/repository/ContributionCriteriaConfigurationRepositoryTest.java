package org.folio.innreach.repository;

import liquibase.pro.packaged.D;
import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.folio.innreach.domain.entity.ContributionCriteriaExcludedLocation;
import org.folio.innreach.domain.entity.ContributionCriteriaStatisticalCodeBehavior;
import org.folio.innreach.fixture.ContributionCriteriaConfigurationFixture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContributionCriteriaConfigurationRepositoryTest extends BaseRepositoryTest {

  private static String CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final List<UUID> UUUID_IDs = List.of(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID());


  private static int TEST_DEFINED_QUANTITY_OF_CONFIGURATIONS = 100;

  private static List<UUID> TEST_DEFINED_CONFIGURATION_IDs = new ArrayList<>(TEST_DEFINED_QUANTITY_OF_CONFIGURATIONS);

  private static List<UUID> TEST_DEFINED_LOCATION_UUIDs = new ArrayList<>();

  private static int TEST_DEFINED_QUATITY_OF_LOCATIONS = 200;

  private static int TEST_DEFINED_QUANTITY_OF_LOCATION_SETS = 50;

  private static int TEST_DEFINED_MAX_SIZE_OF_LOCATION_SET = 50;

  private static List<List<ContributionCriteriaExcludedLocation>> TEST_DEFINED_LOCATIONS_SETs = new ArrayList<>(TEST_DEFINED_QUANTITY_OF_LOCATION_SETS);

  private static Random random = new Random();


  @Autowired
  private CentralServerRepository centralServerRepository;

  @Autowired
  private ContributionCriteriaConfigurationRepository configurationRepository;

  @Autowired
  private ContributionCriteriaConfigurationRepository contributionCriteriaConfigurationRepository;

  @BeforeAll
  static void init() {

    // ContributionConfiguration UUIDs
    for (int i = 0; i < TEST_DEFINED_QUANTITY_OF_CONFIGURATIONS; i++) {
      TEST_DEFINED_CONFIGURATION_IDs.add(UUID.randomUUID());
    }

    // Location UUIDs
    for (int i = 0; i < TEST_DEFINED_QUATITY_OF_LOCATIONS; i++) {
      TEST_DEFINED_LOCATION_UUIDs.add(UUID.randomUUID());
    }

    // location sets
    for (int i = 0; i < TEST_DEFINED_QUANTITY_OF_LOCATION_SETS; i++) {
      List<ContributionCriteriaExcludedLocation> locationSet = new ArrayList<>();

      for (int j = 0; j < myRandom(3,TEST_DEFINED_MAX_SIZE_OF_LOCATION_SET); j++) {
        ContributionCriteriaExcludedLocation exLocation = new ContributionCriteriaExcludedLocation();
        exLocation.setExcludedLocationId(TEST_DEFINED_LOCATION_UUIDs.get(myRandom(0,TEST_DEFINED_QUATITY_OF_LOCATIONS)));
        locationSet.add(exLocation);
      }
      TEST_DEFINED_LOCATIONS_SETs.add(locationSet);
    }
  }

  @BeforeEach
  void beforeEach() {
    ContributionCriteriaConfiguration criteriaConfiguration
      = ContributionCriteriaConfigurationFixture
      .createTestContributionCriteriaConfiguration(UUID.fromString(CENTRAL_SERVER_ID));
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
  }

  @Test
  void updateContributionCriteriaExcludedLocationsStressTest() {
    Long startTime = new Date().getTime();
    for (int i = 0; i < TEST_DEFINED_QUANTITY_OF_CONFIGURATIONS; i++) {
      ContributionCriteriaConfiguration configuration = new ContributionCriteriaConfiguration();
      configuration.setCentralServeId(TEST_DEFINED_CONFIGURATION_IDs.get(i));
      int randomLocationSetIndex = (int) Math.random()*TEST_DEFINED_QUANTITY_OF_LOCATION_SETS;
      List<ContributionCriteriaExcludedLocation> excludedLocations = randomGetTestPredefinedLocationList();
      configuration.updateExcludedLocations(excludedLocations);
      contributionCriteriaConfigurationRepository.saveAndFlush(configuration);
      var optConfig = contributionCriteriaConfigurationRepository.findById(configuration.getCentralServeId());
      assertTrue(optConfig.isPresent());
      assertEquals(0,differenceBetveenExcludedLocationsCollections(excludedLocations,
        optConfig.get().getExcludedLocations().stream().collect(Collectors.toList())).size());
    }
    for (int i = 0; i < 1000; i++) {
      var optConfig
        = contributionCriteriaConfigurationRepository.findById(TEST_DEFINED_CONFIGURATION_IDs.get((int)(Math.random()*TEST_DEFINED_QUANTITY_OF_CONFIGURATIONS)));
      assertTrue(optConfig.isPresent());
      var excludedLocations = randomGetTestPredefinedLocationList();
      optConfig.get().updateExcludedLocations(excludedLocations);
      optConfig
        = contributionCriteriaConfigurationRepository.findById(optConfig.get().getCentralServeId());
      assertEquals(0,differenceBetveenExcludedLocationsCollections(excludedLocations,
        optConfig.get().getExcludedLocations().stream().collect(Collectors.toList())).size());
    }
    System.out.println(("Test time:"+(new Date().getTime()-startTime)/1000));
  }

  private static int myRandom(int min, int max) {
//    String tString = "" + new Date().getTime();
    Double dr = random.nextGaussian();
    String tString = String.valueOf(dr);
    if (tString.contains("E")) tString = tString.substring(0,tString.indexOf("E"));
    int len = (""+max).length();
    String sValue = tString.substring(tString.length()-len);
    int result = Integer.parseInt(sValue);
    while (result>max-1) result = result + min - max/2;
    if (result<min) result = result + min;
    return result;
  }

  private List<ContributionCriteriaExcludedLocation> randomGetTestPredefinedLocationList() {
    int randomIndexOfLocationSet = myRandom(0,TEST_DEFINED_QUANTITY_OF_LOCATION_SETS);
    return TEST_DEFINED_LOCATIONS_SETs.get(randomIndexOfLocationSet);
  }




  @Test
  void chekIfContributionCriteriaConfigurationCreated() {
    var optionalContributionCriteriaConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID));
    assertTrue(optionalContributionCriteriaConfiguration.isPresent());
  }

  @Test
  void deleteContributionCriteriaConfiguration() {
    contributionCriteriaConfigurationRepository.deleteById(UUID.fromString(CENTRAL_SERVER_ID));
    assertTrue(contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).isEmpty());
  }


  @Test
  void createContributionCriteriaConfigurationForExistingCentralServer() {
    ContributionCriteriaConfiguration criteriaConfiguration
      = ContributionCriteriaConfigurationFixture
      .createTestContributionCriteriaConfiguration(UUID.fromString(CENTRAL_SERVER_ID));

    var savedCriteriaConfiguration = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();

    assertNotNull(savedCriteriaConfiguration.getCreatedBy());
    assertNotNull(savedCriteriaConfiguration.getCreatedDate());

    assertEquals(null, savedCriteriaConfiguration.getLastModifiedBy());
    assertEquals(null, savedCriteriaConfiguration.getLastModifiedDate());

    assertTrue(savedCriteriaConfiguration.getExcludedLocations().size()>0);
    assertEquals(criteriaConfiguration.getExcludedLocations().size(),savedCriteriaConfiguration.getExcludedLocations().size());

    assertTrue(savedCriteriaConfiguration.getStatisticalCodeBehaviors().size()>0);
    assertEquals(criteriaConfiguration.getStatisticalCodeBehaviors().size(),savedCriteriaConfiguration.getStatisticalCodeBehaviors().size());
  }

  @Test
  void addContributionCriteriaExcludedLocations() {
    var criteriaConfiguration = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    var quantityOfExcludedLocationsId = criteriaConfiguration.getExcludedLocations().size();
    UUUID_IDs.stream().forEach(uuid -> criteriaConfiguration.addExcludedLocationId(uuid));
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
    contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    var modifiedConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();

    assertEquals(criteriaConfiguration.getCreatedBy(),modifiedConfiguration.getCreatedBy());
    assertEquals(criteriaConfiguration.getCreatedDate(),modifiedConfiguration.getCreatedDate());
    assertNotNull(modifiedConfiguration.getLastModifiedDate());

    assertEquals(quantityOfExcludedLocationsId+ UUUID_IDs.size(),modifiedConfiguration.getExcludedLocations().size());
    List<UUID> excludedLocationIds = modifiedConfiguration.getExcludedLocations().stream().map(excludedLocation -> excludedLocation.getExcludedLocationId()).collect(Collectors.toList());
    assertTrue(UUUID_IDs.stream().filter(uuid -> !excludedLocationIds.contains(uuid)).findAny().isEmpty());
  }

  @Test
  void removeContributionCriteriaExcludedLocations() {
    var criteriaConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    var quantityOfExcludedLocationsId = criteriaConfiguration.getExcludedLocations().size();
    for (int i = 0; i < quantityOfExcludedLocationsId-1 ; i++) {
      criteriaConfiguration.removeExcludedLocationId(
        criteriaConfiguration.getExcludedLocations()
          .stream().findFirst().get().
          getExcludedLocationId());
    }
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
    contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    var modifiedConfiguration = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();

    assertEquals(criteriaConfiguration.getCreatedBy(),modifiedConfiguration.getCreatedBy());
    assertEquals(criteriaConfiguration.getCreatedDate(),modifiedConfiguration.getCreatedDate());
    assertNotNull(modifiedConfiguration.getLastModifiedDate());

    assertEquals(1,modifiedConfiguration.getExcludedLocations().size());
  }

  @Test
  void updateContributionCriteriaExcludedLocations() {
    List<ContributionCriteriaExcludedLocation> mockObtainedFromUiService = new ArrayList<>();
    var criteriaConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    var quantityOfExcludedLocationsId = criteriaConfiguration.getExcludedLocations().size();
    assertEquals(3,criteriaConfiguration.getExcludedLocations().size());
    for (int i = 0; i < 5 ; i++) {
      ContributionCriteriaExcludedLocation excludedLocation = new ContributionCriteriaExcludedLocation();
      excludedLocation.setExcludedLocationId(UUID.randomUUID());
      mockObtainedFromUiService.add(excludedLocation);
    }
    criteriaConfiguration.updateExcludedLocations(mockObtainedFromUiService);
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
    criteriaConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    assertEquals(5,criteriaConfiguration.getExcludedLocations().size());
    assertEquals(0,differenceBetveenExcludedLocationsCollections(mockObtainedFromUiService,criteriaConfiguration.getExcludedLocations().stream().collect(Collectors.toList())).size());

    mockObtainedFromUiService.remove(0);
    mockObtainedFromUiService.remove(0);
    UUUID_IDs.stream().forEach(uuid -> {
      ContributionCriteriaExcludedLocation excludedLocation
        = new ContributionCriteriaExcludedLocation();
      excludedLocation.setExcludedLocationId(uuid);
      mockObtainedFromUiService.add(excludedLocation);
    });
    criteriaConfiguration.updateExcludedLocations(mockObtainedFromUiService);
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
    criteriaConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    assertEquals(5-2+UUUID_IDs.size(),criteriaConfiguration.getExcludedLocations().size());
    assertEquals(0,differenceBetveenExcludedLocationsCollections(mockObtainedFromUiService,criteriaConfiguration.getExcludedLocations().stream().collect(Collectors.toList())).size());

    mockObtainedFromUiService.clear();
    criteriaConfiguration.updateExcludedLocations(mockObtainedFromUiService);
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
    criteriaConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    assertEquals(0,criteriaConfiguration.getExcludedLocations().size());
    assertEquals(0,differenceBetveenExcludedLocationsCollections(mockObtainedFromUiService,criteriaConfiguration.getExcludedLocations().stream().collect(Collectors.toList())).size());
  }

  List<UUID> differenceBetveenExcludedLocationsCollections(List<ContributionCriteriaExcludedLocation> input, List<ContributionCriteriaExcludedLocation> saved) {
    List<UUID> inputIds = input.stream().map(inp -> inp.getExcludedLocationId()).collect(Collectors.toList());
    List<UUID> savedIds = saved.stream().map(sav -> sav.getExcludedLocationId()).collect(Collectors.toList());
    List<UUID> diff1 = inputIds.stream().filter(uuid -> !savedIds.contains(uuid)).collect(Collectors.toList());
    List<UUID> diff2 = savedIds.stream().filter(uuid -> !inputIds.contains(uuid)).collect(Collectors.toList());
    return Stream.concat(diff1.stream(),diff2.stream()).collect(Collectors.toList());
  }


  @Test
  void addContributionCriteriaStatisticalCodeBehavior() {
    var criteriaConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    var quantityOfCodeBehaviors = criteriaConfiguration.getStatisticalCodeBehaviors().size();
    UUUID_IDs.stream().forEach(uuid -> {
      int i = 0;
      ContributionCriteriaStatisticalCodeBehavior codeBehavior = new ContributionCriteriaStatisticalCodeBehavior();
      codeBehavior.setStatisticalCodeId(uuid);
      codeBehavior.setContributionBehavior(ContributionCriteriaStatisticalCodeBehavior.ContributionBehavior.values()[i]);
      criteriaConfiguration.addStatisticalCodeBehavior(codeBehavior);
    });
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
    contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    var modifiedConfiguration = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();

    assertEquals(criteriaConfiguration.getCreatedBy(), modifiedConfiguration.getCreatedBy());
    assertEquals(criteriaConfiguration.getCreatedDate(),modifiedConfiguration.getCreatedDate());
    assertNotNull(modifiedConfiguration.getLastModifiedDate());

    assertEquals(quantityOfCodeBehaviors + UUUID_IDs.size(), modifiedConfiguration.getStatisticalCodeBehaviors().size());
    List<UUID> behaviorUUIDs = modifiedConfiguration.getStatisticalCodeBehaviors().stream().map(statisticalCodeBehavior -> statisticalCodeBehavior.getStatisticalCodeId()).collect(Collectors.toList());
    assertTrue(UUUID_IDs.stream().filter(uuid -> !behaviorUUIDs.contains(uuid)).findAny().isEmpty());
  }

  @Test
  void removeContributionCriteriaStatisticalCodeBehavior() {
    var criteriaConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    var quantityOfStatCodeBehaviors = criteriaConfiguration.getStatisticalCodeBehaviors().size();
    for (int i = 0; i < quantityOfStatCodeBehaviors-1; i++) {
      criteriaConfiguration.removeStatisticalCondeBehavior(
        criteriaConfiguration.getStatisticalCodeBehaviors()
          .stream().findFirst().get().
          getStatisticalCodeId());
    }
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
    var modifiedConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();

    assertEquals(criteriaConfiguration.getCreatedBy(),modifiedConfiguration.getCreatedBy());
    assertEquals(criteriaConfiguration.getCreatedDate(),modifiedConfiguration.getCreatedDate());
    assertNotNull(modifiedConfiguration.getLastModifiedDate());

    assertEquals(1,modifiedConfiguration.getStatisticalCodeBehaviors().size());
  }
}
