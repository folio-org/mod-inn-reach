package org.folio.innreach.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@Table(name = "transaction_item_hold")
@PrimaryKeyJoinColumn(name = "id")
@ToString
public class TransactionItemHold extends TransactionHold {

}
