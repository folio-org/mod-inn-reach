package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.MetaData;
import org.folio.innreach.domain.entity.recordscontribution.ContributionCriteriaConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("sav")
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@Sql(scripts = "classpath:db/pre-populate-central-server.sql")
class ContributionCriteriaConfigurationRepositoryTest {
  @Autowired
  ContributionCriteriaConfigurationRepository contributionCriteriaConfigurationRepository;
  @BeforeEach
  void setUp() {
  }

  @Test
  void createContributionCriteriaConfiguration() {
    ContributionCriteriaConfiguration contributionCriteriaConfiguration
      = new ContributionCriteriaConfiguration();
//    contributionCriteriaConfiguration.setExcludedLocations(Li);
    contributionCriteriaConfiguration.setMetaData(new MetaData());

//    contributionCriteriaConfigurationRepository.save()
  }
}
