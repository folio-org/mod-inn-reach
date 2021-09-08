package org.folio.innreach.domain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "transaction_item_hold")
@ToString
public class TransactionPickupLocation extends Auditable implements Identifiable<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "pickup_loc_code")
  private String pickupLocCode;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "print_name")
  private String printName;

  @Column(name = "delivery_stop")
  private String deliveryStop;
}
