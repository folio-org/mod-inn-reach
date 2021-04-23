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

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = {"id"})
@ToString(exclude = "centralServer")
@Entity
@Table(name = "central_server_credentials")
public class CentralServerCredentials {

  @Id
  private Long id;

  @Column(name = "central_server_key")
  private String centralServerKey;

  @Column(name = "central_server_secret")
  private String centralServerSecret;

  @Column(name = "central_server_secret_salt")
  private String centralServerSecretSalt;

  @Column(name = "local_server_key")
  private String localServerKey;

  @Column(name = "local_server_secret")
  private String localServerSecret;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;
}
