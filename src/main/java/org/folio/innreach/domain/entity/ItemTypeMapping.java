package org.folio.innreach.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "item_type_mapping")
public class ItemTypeMapping  extends Auditable<String> implements Identifiable<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "central_item_type")
  private Integer centralItemType;

  @Column(name = "material_type_id")
  private UUID materialTypeId;

  @Column(name = "local_server_code")
  private String localServerCode;

  @Column(name = "inn_reach_central_server_id")
  private UUID innReachCentralServerId;
}
