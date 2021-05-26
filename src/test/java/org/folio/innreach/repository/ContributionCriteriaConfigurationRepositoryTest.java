package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.folio.innreach.domain.entity.ContributionCriteriaStatisticalCodeBehavior;
import org.folio.innreach.fixture.ContributionCriteriaConfigurationFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Sql(scripts = "classpath:db/pre-populate-central-server.sql")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContributionCriteriaConfigurationRepositoryTest extends BaseRepositoryTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_CENTRAL_SERVER_CODE = "abc12";
  private static final List<UUID> UUUID_IDs = List.of(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID());

  @Autowired
  private CentralServerRepository centralServerRepository;

  @Autowired
  private ContributionCriteriaConfigurationRepository configurationRepository;

  @Autowired
  private ContributionCriteriaConfigurationRepository contributionCriteriaConfigurationRepository;

  @BeforeEach
  void beforeEach() {
    CentralServer centralServer = centralServerRepository.fetchOne(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();
    assertNotNull(centralServer);
    ContributionCriteriaConfiguration criteriaConfiguration
      = ContributionCriteriaConfigurationFixture
      .createTestContributionCriteriaConfiguration(centralServer.getId());
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
  }

  @Test
  void chekIfContributionCriteriaConfigurationCreated() {
    var centralServer = centralServerRepository.fetchOne(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();
    var optionalContributionCriteriaConfiguration = contributionCriteriaConfigurationRepository.findById(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID));
    assertTrue(optionalContributionCriteriaConfiguration.isPresent());
  }

  @Test
  void deleteContributionCriteriaConfiguration() {
    var centralServer = centralServerRepository.fetchOne(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();
    contributionCriteriaConfigurationRepository.deleteById(centralServer.getId());
    assertTrue(contributionCriteriaConfigurationRepository.findById(centralServer.getId()).isEmpty());
  }



  @Test
  void createContributionCriteriaConfigurationForExistingCentralServer() {
    CentralServer centralServer = centralServerRepository.fetchOne(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();
    ContributionCriteriaConfiguration criteriaConfiguration
      = ContributionCriteriaConfigurationFixture
      .createTestContributionCriteriaConfiguration(centralServer.getId());

    var savedCriteriaConfiguration = contributionCriteriaConfigurationRepository.findById(centralServer.getId()).get();

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
    CentralServer centralServer = centralServerRepository.fetchOne(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();
    var criteriaConfiguration = contributionCriteriaConfigurationRepository.findById(centralServer.getId()).get();
    var quantityOfExcludedLocationsId = criteriaConfiguration.getExcludedLocations().size();
    UUUID_IDs.stream().forEach(uuid -> criteriaConfiguration.addExcludedLocationId(uuid));
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
    contributionCriteriaConfigurationRepository.findById(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();
    var modifiedConfiguration = contributionCriteriaConfigurationRepository.findById(centralServer.getId()).get();

    assertEquals(criteriaConfiguration.getCreatedBy(),modifiedConfiguration.getCreatedBy());
    assertNotNull(criteriaConfiguration.getCreatedDate().equals(modifiedConfiguration.getCreatedDate()));
    assertNotNull(modifiedConfiguration.getLastModifiedDate());

    assertEquals(quantityOfExcludedLocationsId+ UUUID_IDs.size(),modifiedConfiguration.getExcludedLocations().size());
    List<UUID> excludedLocationIds = modifiedConfiguration.getExcludedLocations().stream().map(excludedLocation -> excludedLocation.getExcludedLocationId()).collect(Collectors.toList());
    assertTrue(UUUID_IDs.stream().filter(uuid -> !excludedLocationIds.contains(uuid)).findAny().isEmpty());
  }

  @Test
  void removeContributionCriteriaExcludedLocations() {
    CentralServer centralServer = centralServerRepository.fetchOne(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();
    var criteriaConfiguration = contributionCriteriaConfigurationRepository.findById(centralServer.getId()).get();
    var quantityOfExcludedLocationsId = criteriaConfiguration.getExcludedLocations().size();
    for (int i = 0; i < quantityOfExcludedLocationsId-1 ; i++) {
      criteriaConfiguration.removeExcludedLocationId(
        criteriaConfiguration.getExcludedLocations()
          .stream().findFirst().get().
          getExcludedLocationId());
    }
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
    contributionCriteriaConfigurationRepository.findById(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();
    var modifiedConfiguration = contributionCriteriaConfigurationRepository.findById(centralServer.getId()).get();

    assertEquals(criteriaConfiguration.getCreatedBy(),modifiedConfiguration.getCreatedBy());
    assertEquals(criteriaConfiguration.getCreatedDate(),modifiedConfiguration.getCreatedDate());
    assertNotNull(modifiedConfiguration.getLastModifiedDate());

    assertEquals(1,modifiedConfiguration.getExcludedLocations().size());
  }

  @Test
  void addContributionCriteriaStatisticalCodeBehavior() {
    CentralServer centralServer = centralServerRepository.fetchOne(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();
    var criteriaConfiguration = contributionCriteriaConfigurationRepository.findById(centralServer.getId()).get();
    var quantityOfCodeBehaviors = criteriaConfiguration.getStatisticalCodeBehaviors().size();
    UUUID_IDs.stream().forEach(uuid -> {
      int i = 0;
      ContributionCriteriaStatisticalCodeBehavior codeBehavior = new ContributionCriteriaStatisticalCodeBehavior();
      codeBehavior.setStatisticalCodeId(uuid);
      codeBehavior.setContributionBehavior(ContributionCriteriaStatisticalCodeBehavior.ContributionBehavior.values()[i]);
      criteriaConfiguration.addStatisticalCodeBehavior(codeBehavior);
    });
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
    contributionCriteriaConfigurationRepository.findById(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();
    var modifiedConfiguration = contributionCriteriaConfigurationRepository.findById(centralServer.getId()).get();

    assertEquals(criteriaConfiguration.getCreatedBy(), modifiedConfiguration.getCreatedBy());
    assertEquals(criteriaConfiguration.getCreatedDate(),modifiedConfiguration.getCreatedDate());
    assertNotNull(modifiedConfiguration.getLastModifiedDate());

    assertEquals(quantityOfCodeBehaviors + UUUID_IDs.size(), modifiedConfiguration.getStatisticalCodeBehaviors().size());
    List<UUID> behaviorUUIDs = modifiedConfiguration.getStatisticalCodeBehaviors().stream().map(statisticalCodeBehavior -> statisticalCodeBehavior.getStatisticalCodeId()).collect(Collectors.toList());
    assertTrue(UUUID_IDs.stream().filter(uuid -> !behaviorUUIDs.contains(uuid)).findAny().isEmpty());
  }

  @Test
  void removeContributionCriteriaStatisticalCodeBehavior() {
    CentralServer centralServer = centralServerRepository.fetchOne(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();
    var criteriaConfiguration = contributionCriteriaConfigurationRepository.findById(centralServer.getId()).get();
    var quantityOfStatCodeBehaviors = criteriaConfiguration.getStatisticalCodeBehaviors().size();
    for (int i = 0; i < quantityOfStatCodeBehaviors-1 ; i++) {
      criteriaConfiguration.removeStatisticalCondeBehavior(
        criteriaConfiguration.getStatisticalCodeBehaviors()
          .stream().findFirst().get().
          getStatisticalCodeId());
    }
    contributionCriteriaConfigurationRepository.saveAndFlush(criteriaConfiguration);
    var modifiedConfiguration = contributionCriteriaConfigurationRepository.findById(centralServer.getId()).get();

    assertEquals(criteriaConfiguration.getCreatedBy(),modifiedConfiguration.getCreatedBy());
    assertEquals(criteriaConfiguration.getCreatedDate().modifiedConfiguration.getCreatedDate());
    assertNotNull(modifiedConfiguration.getLastModifiedDate());

    assertEquals(1,modifiedConfiguration.getStatisticalCodeBehaviors().size());
  }



}
