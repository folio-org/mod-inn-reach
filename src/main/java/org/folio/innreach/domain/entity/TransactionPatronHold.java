package org.folio.innreach.domain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@Table(name = "transaction_patron_hold")
@PrimaryKeyJoinColumn(name = "id")
@ToString
public class TransactionPatronHold extends TransactionHold {

  @Column(name = "title")
  private String title;

  @Column(name = "author")
  private String author;

  @Column(name = "call_number")
  private String callNumber;

  @Column(name = "shipped_item_barcode")
  private String shippedItemBarcode;
}
