package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContributionCriteriaConfigurationRepository extends JpaRepository<ContributionCriteriaConfiguration, UUID> {
//public interface ContributionCriteriaConfigurationRepository extends CrudRepository<ContributionCriteriaConfiguration, UUID> {
//  @Query(name = ContributionCriteriaConfiguration.FETCH_ALL_QUERY_NAME)
//  List<ContributionCriteriaConfiguration> fetchAll();

//  @Query(name = ContributionCriteriaConfiguration.FETCH_ONE_BY_ID_QUERY_NAME)
//  Optional<ContributionCriteriaConfiguration> fetchOne(UUID centralServerId);
}
