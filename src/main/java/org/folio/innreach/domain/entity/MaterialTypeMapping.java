package org.folio.innreach.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

@Getter
@Setter
@EqualsAndHashCode(of = {"centralItemType", "materialTypeId"}, callSuper = false)
@ToString(exclude = {"centralServer"}, callSuper = true)
@Entity
@Table(name = "material_type_mapping")
public class MaterialTypeMapping extends Auditable implements Identifiable<UUID> {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;
  private UUID materialTypeId;
  private Integer centralItemType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;

}
