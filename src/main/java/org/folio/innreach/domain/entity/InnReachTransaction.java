package org.folio.innreach.domain.entity;

import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_ONE_BY_ID_QUERY;
import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_ONE_BY_ID_QUERY_NAME;
import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_ONE_BY_ITEM_BARCODE_QUERY;
import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_ONE_BY_ITEM_BARCODE_QUERY_NAME;
import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_ONE_BY_TRACKING_ID_AND_CENTRAL_CODE_QUERY;
import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_ONE_BY_TRACKING_ID_AND_CENTRAL_CODE_QUERY_NAME;
import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_ONE_BY_TRACKING_ID_QUERY;
import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_ONE_BY_TRACKING_ID_QUERY_NAME;
import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_OPEN_BY_ITEM_AND_PATRON_QUERY;
import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_OPEN_BY_ITEM_AND_PATRON_QUERY_NAME;
import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_ASSOCIATED_QUERY;
import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_ASSOCIATED_QUERY_NAME;

import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

@Entity
@Getter
@Setter
@ToString(exclude = "hold")
@Table(name = "inn_reach_transaction")
@NamedQuery(
  name = FETCH_ONE_BY_TRACKING_ID_QUERY_NAME,
  query = FETCH_ONE_BY_TRACKING_ID_QUERY
)
@NamedQuery(
  name = FETCH_ONE_BY_ID_QUERY_NAME,
  query = FETCH_ONE_BY_ID_QUERY
)
@NamedQuery(
  name = FETCH_ONE_BY_ITEM_BARCODE_QUERY_NAME,
  query = FETCH_ONE_BY_ITEM_BARCODE_QUERY
)
@NamedQuery(
  name = FETCH_ONE_BY_TRACKING_ID_AND_CENTRAL_CODE_QUERY_NAME,
  query = FETCH_ONE_BY_TRACKING_ID_AND_CENTRAL_CODE_QUERY
)
@NamedQuery(
  name = FETCH_OPEN_BY_ITEM_AND_PATRON_QUERY_NAME,
  query = FETCH_OPEN_BY_ITEM_AND_PATRON_QUERY
)
@NamedQuery(
  name = FETCH_ASSOCIATED_QUERY_NAME,
  query = FETCH_ASSOCIATED_QUERY
)
public class InnReachTransaction extends Auditable implements Identifiable<UUID> {

  public static final String GET_ALL_QUERY_NAME = "InnReachTransaction.getAll";
  public static final String GET_ALL_QUERY = "SELECT t FROM InnReachTransaction t JOIN FETCH t.hold hold " +
    "JOIN FETCH hold.pickupLocation location";

  public static final String FETCH_ONE_BY_TRACKING_ID_QUERY_NAME = "InnReachTransaction.fetchOne";
  public static final String FETCH_ONE_BY_TRACKING_ID_QUERY = GET_ALL_QUERY + " WHERE t.trackingId = :trackingId AND location.id = hold.pickupLocation.id";

  public static final String FETCH_ONE_BY_ID_QUERY_NAME = "InnReachTransaction.fetchOneById";
  public static final String FETCH_ONE_BY_ID_QUERY = GET_ALL_QUERY + " WHERE t.id = :id";

  public static final String FETCH_ONE_BY_ITEM_BARCODE_QUERY_NAME = "InnReachTransaction.fetchOneByItemBarcode";
  public static final String FETCH_ONE_BY_ITEM_BARCODE_QUERY = GET_ALL_QUERY + " WHERE hold.folioItemBarcode = :itemBarcode";

  public static final String FETCH_ONE_BY_TRACKING_ID_AND_CENTRAL_CODE_QUERY_NAME = "InnReachTransaction.fetchByTrackingIdAndCentralCode";
  public static final String FETCH_ONE_BY_TRACKING_ID_AND_CENTRAL_CODE_QUERY = "SELECT irt FROM InnReachTransaction AS irt " +
    "JOIN FETCH irt.hold AS h " +
    "JOIN FETCH h.pickupLocation " +
    "WHERE irt.trackingId = :trackingId AND irt.centralServerCode = :centralServerCode";

  public static final String FETCH_OPEN_BY_ITEM_AND_PATRON_QUERY_NAME = "InnReachTransaction.fetchOpenByFolioItemIdAndPatronId";
  public static final String FETCH_OPEN_BY_ITEM_AND_PATRON_QUERY = "SELECT irt FROM InnReachTransaction AS irt " +
    "JOIN FETCH irt.hold AS h " +
    "JOIN FETCH h.pickupLocation " +
    "WHERE h.folioItemId = :folioItemId AND h.folioPatronId = :folioPatronId AND irt.state NOT IN (4, 11, 12, 13)";

  public static final String FETCH_ASSOCIATED_QUERY_NAME = "InnReachTransaction.fetchAssociatedLoan";
  public static final String FETCH_ASSOCIATED_QUERY = "SELECT irt FROM InnReachTransaction AS irt " +
    "JOIN FETCH irt.hold AS h " +
    "JOIN FETCH h.pickupLocation " +
    "WHERE h.folioLoanId = :folioLoanId";

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "tracking_id")
  private String trackingId;

  @Column(name = "central_server_code")
  private String centralServerCode;

  @Column(name = "state")
  private TransactionState state;

  @Column(name = "type")
  private TransactionType type;

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "transaction_hold_id", unique = true)
  private TransactionHold hold;

  public enum TransactionState {
    ITEM_HOLD,
    PATRON_HOLD,
    LOCAL_HOLD,
    BORROWER_RENEW,
    BORROWING_SITE_CANCEL,
    ITEM_IN_TRANSIT,
    RECEIVE_UNANNOUNCED,
    RETURN_UNCIRCULATED,
    CLAIMS_RETURNED,
    ITEM_RECEIVED,
    ITEM_SHIPPED,
    LOCAL_CHECKOUT,
    CANCEL_REQUEST,
    FINAL_CHECKIN,
    RECALL,
    TRANSFER,
    OWNER_RENEW
  }

  public enum TransactionType {
    ITEM,
    PATRON,
    LOCAL
  }
}
