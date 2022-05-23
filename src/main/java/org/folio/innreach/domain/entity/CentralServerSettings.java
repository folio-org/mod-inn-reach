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
import javax.persistence.NamedQuery;

import org.folio.innreach.domain.entity.base.Identifiable;

import static org.folio.innreach.domain.entity.CentralServerSettings.FIND_BY_CENTRAL_SERVER_ID_QUERY;
import static org.folio.innreach.domain.entity.CentralServerSettings.FIND_BY_CENTRAL_SERVER_ID_QUERY_NAME;

@Getter
@Setter
@EqualsAndHashCode(of = {"check"})
@ToString(exclude = "centralServer")
@Entity
@Table(name = "central_server_settings")
@NamedQuery(
  name = FIND_BY_CENTRAL_SERVER_ID_QUERY_NAME,
  query = FIND_BY_CENTRAL_SERVER_ID_QUERY
)
public class CentralServerSettings implements Identifiable<UUID> {

  public static final String FIND_BY_CENTRAL_SERVER_ID_QUERY_NAME = "CentralServerSettings.findByCentralServerId";
  public static final String FIND_BY_CENTRAL_SERVER_ID_QUERY =
    "SELECT css FROM CentralServerSettings AS css WHERE css.centralServer.id = :centralServerId";

  @Id
  private UUID id;

  @Column(name = "check_pickup_location")
  private boolean check;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;
}
