package org.folio.innreach.domain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "inn_reach_transaction")
@ToString(exclude = {"centralServer"})
public class InnReachTransaction extends Auditable implements Identifiable<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;

  @Column(name = "state")
  private TransactionState state;

  @OneToOne(fetch = FetchType.LAZY)
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
}
