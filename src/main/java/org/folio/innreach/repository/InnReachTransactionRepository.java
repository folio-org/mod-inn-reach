package org.folio.innreach.repository;

import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_OPEN_BY_ITEM_AND_PATRON_QUERY_NAME;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import org.folio.innreach.domain.entity.InnReachTransaction;

import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_ASSOCIATED_QUERY_NAME;

public interface InnReachTransactionRepository extends JpaRepository<InnReachTransaction, UUID>,
  JpaSpecificationExecutor<InnReachTransaction> {

  @Query(name = InnReachTransaction.FETCH_ONE_BY_TRACKING_ID_QUERY_NAME)
  Optional<InnReachTransaction> fetchOneByTrackingId(String trackingId);

  @Query(name = InnReachTransaction.FETCH_ONE_BY_ID_QUERY_NAME)
  Optional<InnReachTransaction> fetchOneById(UUID id);

  @Query(name = InnReachTransaction.FETCH_ONE_BY_ITEM_BARCODE_QUERY_NAME)
  Optional<InnReachTransaction> fetchOneByFolioItemBarcode(String itemBarcode);

  @Query(name = InnReachTransaction.FETCH_ONE_BY_TRACKING_ID_AND_CENTRAL_CODE_QUERY_NAME)
  Optional<InnReachTransaction> findByTrackingIdAndCentralServerCode(String trackingId, String centralServerCode);

  @Query(name = FETCH_OPEN_BY_ITEM_AND_PATRON_QUERY_NAME)
  Optional<InnReachTransaction> fetchOpenByFolioItemIdAndPatronId(UUID folioItemId, UUID folioPatronId);


  @Query(name = FETCH_ASSOCIATED_QUERY_NAME)
  Optional<InnReachTransaction> fetchOneByLoanId(UUID folioLoanId);
}
