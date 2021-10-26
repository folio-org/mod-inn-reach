package org.folio.innreach.domain.entity;

import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_ONE_BY_ID_QUERY;
import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_ONE_BY_ID_QUERY_NAME;
import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_ONE_BY_TRACKING_ID_QUERY;
import static org.folio.innreach.domain.entity.InnReachTransaction.FETCH_ONE_BY_TRACKING_ID_QUERY_NAME;
import static org.folio.innreach.domain.entity.InnReachTransaction.GET_ALL_SORTED_QUERY;
import static org.folio.innreach.domain.entity.InnReachTransaction.GET_ALL_SORTED_QUERY_NAME;
import static org.folio.innreach.domain.entity.InnReachTransaction.GET_ALL_SORTED_COUNT_QUERY_NAME;
import static org.folio.innreach.domain.entity.InnReachTransaction.GET_ALL_SORTED_COUNT_QUERY;

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

import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

@Entity
@Getter
@Setter
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
  name = GET_ALL_SORTED_QUERY_NAME,
  query = GET_ALL_SORTED_QUERY
)
@NamedQuery(
  name = GET_ALL_SORTED_COUNT_QUERY_NAME,
  query = GET_ALL_SORTED_COUNT_QUERY
)
public class InnReachTransaction extends Auditable implements Identifiable<UUID> {

  public static final String FETCH_ONE_QUERY = "SELECT t FROM InnReachTransaction t JOIN FETCH t.hold hold " +
    "JOIN FETCH hold.pickupLocation location";

  public static final String FETCH_ONE_BY_TRACKING_ID_QUERY_NAME = "InnReachTransaction.fetchOne";
  public static final String FETCH_ONE_BY_TRACKING_ID_QUERY = FETCH_ONE_QUERY + " WHERE t.trackingId = :trackingId AND location.id = hold.pickupLocation.id";

  public static final String FETCH_ONE_BY_ID_QUERY_NAME = "InnReachTransaction.fetchOneById";
  public static final String FETCH_ONE_BY_ID_QUERY = FETCH_ONE_QUERY + " WHERE t.id = :id";

  public static final String GET_ALL_SORTED_QUERY_NAME = "InnReachTransaction.getAllSorted";
  public static final String GET_ALL_SORTED_QUERY = "SELECT t FROM InnReachTransaction t JOIN FETCH t.hold hold " +
    "JOIN FETCH hold.pickupLocation location";

  public static final String GET_ALL_SORTED_COUNT_QUERY_NAME = "InnReachTransaction.getAllCount";
  public static final String GET_ALL_SORTED_COUNT_QUERY = "SELECT count(transaction) FROM InnReachTransaction transaction ";

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
    TRANSFER
  }

  public enum TransactionType {
    ITEM,
    PATRON,
    LOCAL
  }
}
