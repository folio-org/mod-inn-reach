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
@Table(name = "transaction_local_hold")
@PrimaryKeyJoinColumn(name = "id")
@ToString
public class TransactionLocalHold extends TransactionHold {

  @Column(name = "patron_home_library")
  private String patronHomeLibrary;

  @Column(name = "patron_phone")
  private String patronPhone;

  @Column(name = "title")
  private String title;

  @Column(name = "author")
  private String author;

  @Column(name = "callNumber")
  private String callNumber;

  @Column(name = "central_patron_type")
  private Integer centralPatronTypeLocal;

  @Column(name = "patron_name")
  private String patronName;
}
