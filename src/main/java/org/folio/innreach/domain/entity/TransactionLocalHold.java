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
@Table(name = "transaction_local_hold")
@PrimaryKeyJoinColumn(name = "id")
@ToString
public class TransactionLocalHold extends TransactionHold {

  @Column(name = "patron_home_library")
  private String patronHomeLibrary;

  @Column(name = "patron_phone")
  private String patronPhone;

  @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
  @Column(name = "title")
  private String titleLocal;

  @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
  @Column(name = "author")
  private String authorLocal;

  @Column(name = "callNumber")
  private String callNumber;

  @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
  @Column(name = "central_patron_type")
  private Integer centralPatronTypeLocal;

  @Column(name = "patron_name")
  private String patronName;

  public String getTitle() {
    return titleLocal;
  }

  public void setTitle(String title) {
    this.titleLocal = title;
  }

  public String getAuthor() {
    return authorLocal;
  }

  public void setAuthor(String author) {
    this.authorLocal = author;
  }

  public Integer getCentralPatronType() {
    return centralPatronTypeLocal;
  }

  public void setCentralPatronType(Integer centralPatronType) {
    this.centralPatronTypeLocal = centralPatronType;
  }
}
