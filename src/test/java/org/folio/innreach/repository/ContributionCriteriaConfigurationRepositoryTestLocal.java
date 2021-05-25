package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.folio.innreach.fixture.CentralServerCredentialsFixture;
import org.folio.innreach.fixture.CentralServerFixture;
import org.folio.innreach.fixture.ContributionCriteriaConfigurationFixture;
import org.folio.innreach.fixture.LocalAgencyFixture;
import org.folio.innreach.fixture.LocalServerCredentialsFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Sql(scripts = "classpath:db/pre-populate-central-server.sql")
@Rollback(value = false)
class ContributionCriteriaConfigurationRepositoryTestLocal extends BaseRepositoryTestLocal {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_CENTRAL_SERVER_CODE = "abc12";


  @Autowired
  private CentralServerRepository centralServerRepository;
  @Autowired
  private ContributionCriteriaConfigurationRepository contributionCriteriaConfigurationRepository;

  @Test
  void saveCentralServer_when_suchCentralServerDoesNotExist() {
    var centralServer = CentralServerFixture.createCentralServer();
    centralServer.setCentralServerCredentials(CentralServerCredentialsFixture.createCentralServerCredentials());
    centralServer.setLocalServerCredentials(LocalServerCredentialsFixture.createLocalServerCredentials());

    centralServer.addLocalAgency(LocalAgencyFixture.createLocalAgency());
    centralServer.addLocalAgency(LocalAgencyFixture.createLocalAgency());
    centralServer.addLocalAgency(LocalAgencyFixture.createLocalAgency());

    var savedCentralServer = centralServerRepository.save(centralServer);

    assertNotNull(savedCentralServer);
    assertNotNull(savedCentralServer.getId());
    assertNotNull(savedCentralServer.getCentralServerCredentials());
    assertNotNull(savedCentralServer.getLocalServerCredentials());
  }

  @Test
  void getCentralServer_when_centralServerExists() {
    var centralServerById = centralServerRepository.getOne(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID));

    assertNotNull(centralServerById.getLocalServerCredentials().getLocalServerKey());
    assertNotNull(centralServerById.getCentralServerCredentials().getCentralServerKey());
    assertNotNull(centralServerById.getLocalAgencies());
    assertFalse(centralServerById.getLocalAgencies().isEmpty());
    assertFalse(centralServerById.getLocalAgencies().get(0).getFolioLibraryIds().isEmpty());
  }

  @Test
  void createContributionCriteriaConfiguration(){
    var configuration = ContributionCriteriaConfigurationFixture.createTestContributionCriteriaConfiguration(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID));
    var savedConfiguration = contributionCriteriaConfigurationRepository.saveAndFlush(configuration);
    assertNotNull(savedConfiguration);
    assertEquals(ContributionCriteriaConfigurationFixture.EXCLUDED_LOCATION_IDs.length,savedConfiguration.getExcludedLocations().size());
    List<ContributionCriteriaConfiguration> configurations = contributionCriteriaConfigurationRepository.findAll();
    assertNotNull(configurations);
    assertTrue(configurations.size()>0);
    var savedConfig = configurations.get(0);
    UUID locationIdForCheckAddDelete =  savedConfig.getExcludedLocations().stream().findAny().get().getExcludedLocationId();
    savedConfig.addExcludedLocationId(locationIdForCheckAddDelete);
    savedConfig = contributionCriteriaConfigurationRepository.save(savedConfig);
    savedConfig.removeExcludedLocationId(locationIdForCheckAddDelete);
    savedConfig = savedConfig = contributionCriteriaConfigurationRepository.save(savedConfig);
    var savedContributionCriteria = contributionCriteriaConfigurationRepository.findById(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();
    var excludedLocatioIds = savedContributionCriteria.getExcludedLocations();
    var quantityOfExcludedLocations = excludedLocatioIds.size();
    var excludedLocationIdForDelete = excludedLocatioIds.stream().findAny().get().getExcludedLocationId();
    savedContributionCriteria.removeExcludedLocationId(excludedLocationIdForDelete);
    savedContributionCriteria = contributionCriteriaConfigurationRepository.save(savedContributionCriteria);
    assertEquals(quantityOfExcludedLocations-1,savedContributionCriteria.getExcludedLocations().size());
    savedContributionCriteria = contributionCriteriaConfigurationRepository.findById(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();
    var statCodeIdForDelete = savedContributionCriteria.getStatisticalCodeBehaviors().stream().findAny().get().getStatisticalCodeId();
    var quantityOfStatCodeBehavior = savedContributionCriteria.getStatisticalCodeBehaviors().size();
    savedContributionCriteria.removeStatisticalCondeBehavior(statCodeIdForDelete);
    contributionCriteriaConfigurationRepository.save(savedContributionCriteria);
    savedContributionCriteria = contributionCriteriaConfigurationRepository.findById(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();
    assertEquals(quantityOfStatCodeBehavior-1,savedContributionCriteria.getStatisticalCodeBehaviors().size());
  }

  @Test
  void removeExcludedLocation() {

  }


  @Test
  void addExcludedLocation() {
//    contributionCriteriaConfigurationRepository.findById()
  }


//  @Test
//  void throwException_when_saveCentralServerWithoutRequiredData() {
//    var centralServer = CentralServerFixture.createCentralServer();
//    centralServer.setLocalServerCode(null);
//
//    assertThrows(DataIntegrityViolationException.class, () -> centralServerRepository.saveAndFlush(centralServer));
//  }
//
//  @Test
//  void throwException_when_suchCentralServerAlreadyExists() {
//    var centralServer = CentralServerFixture.createCentralServer();
//    centralServer.setLocalServerCode(PRE_POPULATED_CENTRAL_SERVER_CODE);
//
//    assertThrows(DataIntegrityViolationException.class, () -> centralServerRepository.saveAndFlush(centralServer));
//  }

}
