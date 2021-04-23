package org.folio.innreach.domain.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = {"name", "localServerCode"})
@ToString(exclude = "centralServerCredentials")
@Entity
@Table(name = "central_server")
public class CentralServer {

  public CentralServer(Long id,
                       String name,
                       String description,
                       String localServerCode,
                       String centralServerAddress,
                       CentralServerCredentials centralServerCredentials) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.localServerCode = localServerCode;
    this.centralServerAddress = centralServerAddress;
    setCentralServerCredentials(centralServerCredentials);
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String name;
  private String description;

  @Column(name = "local_server_code")
  private String localServerCode;

  @Column(name = "central_server_address")
  private String centralServerAddress;

  @OneToOne(
    cascade = CascadeType.PERSIST,
    fetch = FetchType.LAZY,
    mappedBy = "centralServer",
    orphanRemoval = true
  )
  private CentralServerCredentials centralServerCredentials;

  public void setCentralServerCredentials(CentralServerCredentials centralServerCredentials) {
    if (centralServerCredentials != null) {
      centralServerCredentials.setCentralServer(this);
    }
    this.centralServerCredentials = centralServerCredentials;
  }

}
