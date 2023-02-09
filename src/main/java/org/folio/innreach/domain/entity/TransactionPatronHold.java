package org.folio.innreach.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@Table(name = "transaction_patron_hold")
@PrimaryKeyJoinColumn(name = "id")
@ToString
public class TransactionPatronHold extends TransactionHold {

  @Column(name = "call_number")
  private String callNumber;

  @Column(name = "shipped_item_barcode")
  private String shippedItemBarcode;
}
