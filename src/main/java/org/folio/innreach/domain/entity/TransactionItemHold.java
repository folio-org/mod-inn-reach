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
@Table(name = "transaction_item_hold")
@PrimaryKeyJoinColumn(name = "id")
@ToString
public class TransactionItemHold extends TransactionHold {

  @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
  @Column(name = "central_patron_type")
  private Integer centralPatronTypeItem;

  @Column(name = "patron_name")
  private String patronName;

  public Integer getCentralPatronType() {
    return centralPatronTypeItem;
  }

  public void setCentralPatronType(Integer centralPatronType) {
    this.centralPatronTypeItem = centralPatronType;
  }
}
