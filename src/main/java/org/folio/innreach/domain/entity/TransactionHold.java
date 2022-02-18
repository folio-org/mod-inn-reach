package org.folio.innreach.domain.entity;

import java.util.UUID;

import javax.persistence.CascadeType;
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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

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
  protected Integer transactionTime;

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "pickup_location_id", unique = true)
  protected TransactionPickupLocation pickupLocation;

  @Column(name = "patron_id")
  protected String patronId;

  @Column(name = "patron_agency_code")
  protected String patronAgencyCode;

  @Column(name = "item_agency_code")
  protected String itemAgencyCode;

  @Column(name = "item_id")
  protected String itemId;

  @Column(name = "central_item_type")
  protected Integer centralItemType;

  @Column(name = "need_before")
  protected Integer needBefore;

  @Column(name = "due_date_time")
  protected Integer dueDateTime;

  @Column(name = "title")
  private String title;

  @Column(name = "folio_patron_id")
  protected UUID folioPatronId;

  @Column(name = "folio_instance_id")
  protected UUID folioInstanceId;

  @Column(name = "folio_holding_id")
  protected UUID folioHoldingId;

  @Column(name = "folio_item_id")
  protected UUID folioItemId;

  @Column(name = "folio_request_id")
  protected UUID folioRequestId;

  @Column(name = "folio_loan_id")
  protected UUID folioLoanId;

  @Column(name = "folio_patron_barcode")
  protected String folioPatronBarcode;

  @Column(name = "folio_item_barcode")
  protected String folioItemBarcode;

  @Column(name = "central_patron_type")
  protected Integer centralPatronTypeItem;

  @Column(name = "patron_name")
  protected String patronName;
}
