package org.folio.innreach.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.folio.innreach.domain.entity.TransactionHold;

@Repository
public interface TransactionHoldRepository extends JpaRepository<TransactionHold, UUID> {

  Integer countByPatronIdAndFolioLoanIdIn(String patronId, List<UUID> loanIds);

}
