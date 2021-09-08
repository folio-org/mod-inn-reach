package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InnReachTransactionRepository extends JpaRepository<InnReachTransaction, UUID> {
}
