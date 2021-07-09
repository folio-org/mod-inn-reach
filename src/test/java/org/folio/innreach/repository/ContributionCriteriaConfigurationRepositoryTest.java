package org.folio.innreach.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

//import static org.folio.innreach.fixture.ContributionCriteriaConfigurationFixture.createTestContributionCriteriaConfiguration;

class ContributionCriteriaConfigurationRepositoryTest extends BaseRepositoryTest {

  private static String CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  private static final List<UUID> UUUID_IDs = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

  @Autowired
  private CentralServerRepository centralServerRepository;

  @Autowired
  private ContributionCriteriaConfigurationRepository contributionCriteriaConfigurationRepository;


  /*@BeforeEach
  void beforeEach() {
    ContributionCriteriaConfiguration criteriaConfiguration
      = createTestContributionCriteriaConfiguration(UUID.fromString(CENTRAL_SERVER_ID));
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
  }

  @Test
  void chekIfContributionCriteriaConfigurationCreated() {
    var optionalContributionCriteriaConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID));
    assertTrue(optionalContributionCriteriaConfiguration.isPresent());
  }

  @Test
  void createContributionCriteriaConfigurationForExistingCentralServer() {
    ContributionCriteriaConfiguration criteriaConfiguration
      = createTestContributionCriteriaConfiguration(UUID.fromString(CENTRAL_SERVER_ID));

    var savedCriteriaConfiguration = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();

    assertNotNull(savedCriteriaConfiguration.getCreatedBy());
    assertNotNull(savedCriteriaConfiguration.getCreatedDate());

    assertEquals(null, savedCriteriaConfiguration.getLastModifiedBy());
    assertEquals(null, savedCriteriaConfiguration.getLastModifiedDate());

    assertTrue(savedCriteriaConfiguration.getExcludedLocations().size() > 0);
    assertEquals(criteriaConfiguration.getExcludedLocations().size(), savedCriteriaConfiguration.getExcludedLocations().size());

    assertTrue(savedCriteriaConfiguration.getStatisticalCodeBehaviors().size() > 0);
    assertEquals(criteriaConfiguration.getStatisticalCodeBehaviors().size(), savedCriteriaConfiguration.getStatisticalCodeBehaviors().size());
  }

  @Test
  void addFewContributionCriteriaExcludedLocations() {
    var criteriaConfiguration = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    var quantityOfExcludedLocationsId = criteriaConfiguration.getExcludedLocations().size();
    UUUID_IDs.stream().forEach(uuid -> {
      ContributionCriteriaExcludedLocation excludedLocation = new ContributionCriteriaExcludedLocation();
      excludedLocation.setExcludedLocationId(uuid);
      criteriaConfiguration.addExcludedLocation(excludedLocation);
      }
    );
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
    var modifiedConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    assertEquals(criteriaConfiguration.getCreatedBy(), modifiedConfiguration.getCreatedBy());
    assertEquals(criteriaConfiguration.getCreatedDate(), modifiedConfiguration.getCreatedDate());
    assertNotNull(modifiedConfiguration.getLastModifiedDate());
    assertEquals(quantityOfExcludedLocationsId + UUUID_IDs.size(), modifiedConfiguration.getExcludedLocations().size());
  }

  @Test
  void removeFewContributionCriteriaExcludedLocation() {
    var criteriaConfiguration = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    var quantityOfExcludedLocations = criteriaConfiguration.getExcludedLocations().size();
    var excludedLocationForDelete = criteriaConfiguration.getExcludedLocations().stream().findFirst().get();
    criteriaConfiguration.removeExcludedLocation(excludedLocationForDelete);
    excludedLocationForDelete = criteriaConfiguration.getExcludedLocations().stream().findFirst().get();
    criteriaConfiguration.removeExcludedLocation(excludedLocationForDelete);
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
    var modifiedConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    assertEquals(quantityOfExcludedLocations-2,modifiedConfiguration.getExcludedLocations().size());
  }

  @Test
  void addFewStatisticalCodeBehaviors() {
    var criteriaConfiguration = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    var quantityOfStatisticalCodeBehavior = criteriaConfiguration.getExcludedLocations().size();
    int i = 0;
    for (var uuid : UUUID_IDs) {
      ContributionCriteriaStatisticalCodeBehavior statisticalCodeBehavior = new ContributionCriteriaStatisticalCodeBehavior();
      statisticalCodeBehavior.setStatisticalCodeId(uuid);
      statisticalCodeBehavior.setContributionBehavior(ContributionBehavior.values()[i++]);
      criteriaConfiguration.addStatisticalCodeBehavior(statisticalCodeBehavior);
      if (i > UUUID_IDs.size()) i = 0;
    }

    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
    var modifiedConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    assertEquals(criteriaConfiguration.getCreatedBy(), modifiedConfiguration.getCreatedBy());
    assertEquals(criteriaConfiguration.getCreatedDate(), modifiedConfiguration.getCreatedDate());
    assertNotNull(modifiedConfiguration.getLastModifiedDate());
    assertEquals(quantityOfStatisticalCodeBehavior + UUUID_IDs.size(), modifiedConfiguration.getStatisticalCodeBehaviors().size());
  }


  @Test
  void removeFewExcludedLocations() {
    var criteriaConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    var quantityOfLocations = criteriaConfiguration.getExcludedLocations().size();
    ContributionCriteriaExcludedLocation excludedLocationForDelete
      = criteriaConfiguration.getExcludedLocations().stream().findFirst().get();
    criteriaConfiguration.removeExcludedLocation(excludedLocationForDelete);

    excludedLocationForDelete
      = criteriaConfiguration.getExcludedLocations().stream().findFirst().get();
    criteriaConfiguration.removeExcludedLocation(excludedLocationForDelete);

    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
    var modifiedConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    assertEquals(quantityOfLocations-2,modifiedConfiguration.getExcludedLocations().size());
  }


  @Test
  void removeFewStatisticalCodeBehaviors() {
    var criteriaConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    var quantityOfBehaviors = criteriaConfiguration.getStatisticalCodeBehaviors().size();
    ContributionCriteriaStatisticalCodeBehavior statisticalCodeForDelete
      = criteriaConfiguration.getStatisticalCodeBehaviors().stream().findFirst().get();
    criteriaConfiguration.removeStatisticalCondeBehavior(statisticalCodeForDelete);
    statisticalCodeForDelete
      = criteriaConfiguration.getStatisticalCodeBehaviors().stream().findFirst().get();
    criteriaConfiguration.removeStatisticalCondeBehavior(statisticalCodeForDelete);
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
    var modifiedConfiguration
      = contributionCriteriaConfigurationRepository.findById(UUID.fromString(CENTRAL_SERVER_ID)).get();
    assertEquals(quantityOfBehaviors-2,modifiedConfiguration.getStatisticalCodeBehaviors().size());
  }*/
}
