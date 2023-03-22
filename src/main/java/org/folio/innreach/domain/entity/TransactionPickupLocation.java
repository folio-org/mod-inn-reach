package org.folio.innreach.domain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "transaction_pickup_location")
@ToString
public class TransactionPickupLocation extends Auditable implements Identifiable<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "pickup_loc_code")
  private String pickupLocCode;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "delivery_stop")
  private String deliveryStop;
}
