package org.folio.innreach.repository;

import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_ALL_BY_SHIPPED_ITEM_BARCODE_AND_STATE_IN_COUNT_QUERY_NAME;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import org.folio.innreach.domain.entity.InnReachTransaction;

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

  @Query(name = InnReachTransaction.FETCH_ALL_BY_SHIPPED_ITEM_BARCODE_AND_STATE_IN_QUERY_NAME, countName = FETCH_ALL_BY_SHIPPED_ITEM_BARCODE_AND_STATE_IN_COUNT_QUERY_NAME)
  Page<InnReachTransaction> findByShippedItemBarcodeAndStateIn(String shippedItemBarcode, List<InnReachTransaction.TransactionState> states, Pageable pageable);
}
