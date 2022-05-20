package org.folio.innreach.domain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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

}
