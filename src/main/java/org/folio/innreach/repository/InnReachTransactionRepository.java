package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface InnReachTransactionRepository extends JpaRepository<InnReachTransaction, UUID> {
  @Query(name = InnReachTransaction.FETCH_ONE_BY_TRACKING_ID_QUERY_NAME)
  Optional<InnReachTransaction> fetchOneByTrackingId(String trackingId);
}
