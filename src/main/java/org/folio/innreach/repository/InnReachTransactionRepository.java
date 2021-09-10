package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface InnReachTransactionRepository extends JpaRepository<InnReachTransaction, UUID> {
  @Query(name = InnReachTransaction.FETCH_TRANSACTION_HOLD_NAME)
  Optional<TransactionHold> findTransactionHoldById(UUID id);
}
