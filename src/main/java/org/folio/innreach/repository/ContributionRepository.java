package org.folio.innreach.repository;

import static org.folio.innreach.domain.entity.Contribution.FETCH_HISTORY_COUNT_QUERY_NAME;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.folio.innreach.domain.entity.Contribution;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, UUID> {

  @Query(name = Contribution.FETCH_CURRENT_QUERY_NAME)
  Optional<Contribution> fetchCurrentByCentralServerId(UUID id);

  @Query(name = Contribution.FETCH_HISTORY_QUERY_NAME, countName = FETCH_HISTORY_COUNT_QUERY_NAME)
  Page<Contribution> fetchHistoryByCentralServerId(UUID id, Pageable pageable);

}
