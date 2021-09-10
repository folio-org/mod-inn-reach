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
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "transaction_hold")
@ToString(exclude = {"pickupLocation"})
public abstract class TransactionHold extends Auditable implements Identifiable<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "transaction_time")
  private OffsetDateTime transactionTime;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pickup_location_id", unique = true)
  private TransactionPickupLocation pickupLocation;

  @Column(name = "patron_id")
  private UUID patronId;

  @Column(name = "patron_agency_code")
  private String patronAgencyCode;

  @Column(name = "item_agency_code")
  private String itemAgencyCode;

  @Column(name = "item_id")
  private UUID itemId;

  @Column(name = "central_item_type")
  private Integer centralItemType;

  @Column(name = "need_before")
  private OffsetDateTime needBefore;

  @Column(name = "folio_patron_id")
  private UUID folioPatronId;

  @Column(name = "folio_item_id")
  private UUID folioItemId;

  @Column(name = "folio_request_id")
  private UUID folioRequestId;

  @Column(name = "folio_loan_id")
  private UUID folioLoanId;
}
