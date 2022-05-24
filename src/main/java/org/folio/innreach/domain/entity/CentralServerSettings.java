package org.folio.innreach.domain.entity;

import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.folio.innreach.domain.entity.base.Identifiable;

@Getter
@Setter
@EqualsAndHashCode(of = {"check"})
@ToString(exclude = "centralServer")
@Entity
@Table(name = "central_server_settings")
public class CentralServerSettings implements Identifiable<UUID> {

  @Id
  private UUID id;

  @Column(name = "check_pickup_location")
  private boolean check;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;
}
