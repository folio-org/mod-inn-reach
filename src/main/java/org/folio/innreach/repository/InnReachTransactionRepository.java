package org.folio.innreach.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.folio.innreach.domain.entity.InnReachTransaction;

public interface InnReachTransactionRepository extends JpaRepository<InnReachTransaction, UUID> {
  @Query(name = InnReachTransaction.FETCH_ONE_BY_TRACKING_ID_QUERY_NAME)
  Optional<InnReachTransaction> fetchOneByTrackingId(String trackingId);

  @Query(name = InnReachTransaction.GET_ALL_SORTED_QUERY_NAME, countName = InnReachTransaction.GET_ALL_SORTED_COUNT_QUERY_NAME)
  Page<InnReachTransaction> getAllSorted(Pageable pageable);
}
