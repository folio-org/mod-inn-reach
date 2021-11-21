package org.folio.innreach.domain.entity;

import lombok.AccessLevel;
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

  @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
  @Column(name = "title")
  private String titlePatron;

  @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
  @Column(name = "author")
  private String authorPatron;

  @Column(name = "call_number")
  private String callNumber;

  @Column(name = "shipped_item_barcode")
  private String shippedItemBarcode;

  public String getTitle() {
    return titlePatron;
  }

  public void setTitle(String title) {
    this.titlePatron = title;
  }

  public String getAuthor() {
    return authorPatron;
  }

  public void setAuthor(String author) {
    this.authorPatron = author;
  }
}
