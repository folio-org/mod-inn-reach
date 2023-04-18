package org.folio.innreach.domain.entity;

import static org.folio.innreach.domain.entity.LocalServerCredentials.FIND_BY_LOCAL_SERVER_KEY_QUERY;
import static org.folio.innreach.domain.entity.LocalServerCredentials.FIND_BY_LOCAL_SERVER_KEY_QUERY_NAME;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Identifiable;

/**
 * The key/secret provided by FOLIO for INN-Reach D2IR.
 * Used by INN-Reach D2IR as payload to get JWT access token from FOLIO.
 */

@Getter
@Setter
@EqualsAndHashCode(of = {"localServerKey", "localServerSecret"})
@ToString(exclude = "centralServer")
@Entity
@Table(name = "local_server_credentials")
@NamedQuery(
  name = FIND_BY_LOCAL_SERVER_KEY_QUERY_NAME,
  query = FIND_BY_LOCAL_SERVER_KEY_QUERY
)
public class LocalServerCredentials implements Identifiable<UUID> {

  public static final String FIND_BY_LOCAL_SERVER_KEY_QUERY_NAME = "LocalServerCredentials.findByLocalServerKey";
  public static final String FIND_BY_LOCAL_SERVER_KEY_QUERY = "SELECT lsc FROM LocalServerCredentials AS lsc " +
    "WHERE lsc.localServerKey = :localServerKey";

  @Id
  private UUID id;

  @Column(name = "local_server_key")
  private String localServerKey;

  @Column(name = "local_server_secret")
  private String localServerSecret;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;
}
