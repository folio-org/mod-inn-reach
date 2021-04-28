package org.folio.innreach.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
import java.util.UUID;

/**
 * The key/secret provided by FOLIO for INN-Reach D2IR.
 * Used by INN-Reach D2IR as payload to get JWT access token from FOLIO.
 */

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = {"id"})
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
  private String centralServerSecretSalt;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;
}
