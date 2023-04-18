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
@Table(name = "transaction_local_hold")
@PrimaryKeyJoinColumn(name = "id")
@ToString
public class TransactionLocalHold extends TransactionHold {

  @Column(name = "patron_home_library")
  private String patronHomeLibrary;

  @Column(name = "patron_phone")
  private String patronPhone;

  @Column(name = "callNumber")
  private String callNumber;
}
