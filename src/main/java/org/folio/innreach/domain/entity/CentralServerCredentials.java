package org.folio.innreach.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Identifiable;

/**
 * The key/secret provided by INN-Reach D2IR for FOLIO.
 * Used by FOLIO as payload to get JWT access token from INN-Reach D2IR.
 */

@Getter
@Setter
@EqualsAndHashCode(of = {"centralServerKey", "centralServerSecret"})
@ToString(exclude = "centralServer")
@Entity
@Table(name = "central_server_credentials")
public class CentralServerCredentials implements Identifiable<UUID> {

  @Id
  private UUID id;

  @Column(name = "central_server_key")
  private String centralServerKey;

  @Column(name = "central_server_secret")
  private String centralServerSecret;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;
}
