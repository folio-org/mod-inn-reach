package org.folio.innreach.domain.entity;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Auditable;

@Getter
@Setter
@EqualsAndHashCode(of = "code", callSuper = false)
@ToString(callSuper = true)
@Entity
@Table(name = "inn_reach_location")
public class InnReachLocation extends Auditable<String> {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;
  private String code;
  private String description;
}
