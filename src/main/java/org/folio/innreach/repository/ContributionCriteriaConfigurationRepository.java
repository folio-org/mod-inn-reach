package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContributionCriteriaConfigurationRepository extends JpaRepository<ContributionCriteriaConfiguration, UUID> {
}
