package org.folio.innreach.domain.entity;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Auditable;

@Getter
@Setter
@EqualsAndHashCode(of = {"centralItemType", "materialTypeId"}, callSuper = false)
@ToString(exclude = {"centralServer"}, callSuper = true)
@Entity
@Table(name = "material_type_mapping")
public class MaterialTypeMapping extends Auditable<String> {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;
  private UUID materialTypeId;
  private int centralItemType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;

}
