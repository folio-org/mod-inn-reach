package org.folio.innreach.domain.entity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The key/secret provided by FOLIO for INN-Reach D2IR.
 * Used by INN-Reach D2IR as payload to get JWT access token from FOLIO.
 */

@Getter
@Setter
@EqualsAndHashCode(of = {"localServerKey", "localServerSecret", "localServerSecretSalt"})
@ToString(exclude = "centralServer")
@Entity
@Table(name = "local_server_credentials")
public class LocalServerCredentials {

  @Id
  private UUID id;

  @Column(name = "local_server_key")
  private String localServerKey;

  @Column(name = "local_server_secret")
  private String localServerSecret;

  @Column(name = "local_server_secret_salt")
  private String localServerSecretSalt;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;
}
