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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = {"name", "localServerCode"})
@ToString(exclude = {"centralServerCredentials", "localServerCredentials", "agencies"})
@Entity
@Table(name = "central_server")
public class CentralServer {

  public CentralServer(UUID id,
                       String name,
                       String description,
                       String localServerCode,
                       String centralServerAddress,
                       String loanTypeId,
                       CentralServerCredentials centralServerCredentials,
                       LocalServerCredentials localServerCredentials) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.localServerCode = localServerCode;
    this.centralServerAddress = centralServerAddress;
    this.loanTypeId = loanTypeId;
    setCentralServerCredentials(centralServerCredentials);
    setLocalServerCredentials(localServerCredentials);
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;
  private String name;
  private String description;

  @Column(name = "local_server_code")
  private String localServerCode;

  @Column(name = "central_server_address")
  private String centralServerAddress;

  @Column(name = "loan_type_id")
  private String loanTypeId;

  @OneToOne(
    cascade = CascadeType.ALL,
    fetch = FetchType.LAZY,
    mappedBy = "centralServer",
    orphanRemoval = true,
    optional = false
  )
  private CentralServerCredentials centralServerCredentials;

  @OneToOne(
    cascade = CascadeType.ALL,
    fetch = FetchType.LAZY,
    mappedBy = "centralServer",
    orphanRemoval = true
  )
  private LocalServerCredentials localServerCredentials;

  @OneToMany(
    cascade = CascadeType.PERSIST,
    fetch = FetchType.LAZY,
    mappedBy = "centralServer",
    orphanRemoval = true
  )
  private List<LocalAgency> agencies = new ArrayList<>();

  public void setCentralServerCredentials(CentralServerCredentials centralServerCredentials) {
    if (centralServerCredentials != null) {
      centralServerCredentials.setCentralServer(this);
    }
    this.centralServerCredentials = centralServerCredentials;
  }

  public void setLocalServerCredentials(LocalServerCredentials localServerCredentials) {
    if (localServerCredentials != null) {
      localServerCredentials.setCentralServer(this);
    }
    this.localServerCredentials = localServerCredentials;
  }

  public void addAgency(LocalAgency localAgency) {
    if (localAgency != null) {
      localAgency.setCentralServer(this);
    }
    this.agencies.add(localAgency);
  }

  public void removeAgency(LocalAgency localAgency) {
    if (localAgency != null) {
      localAgency.setCentralServer(null);
    }
    this.agencies.remove(localAgency);
  }

}
