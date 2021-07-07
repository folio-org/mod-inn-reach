package org.folio.innreach.domain.entity;

import static org.folio.innreach.domain.entity.CentralServer.FETCH_ALL_BY_ID_QUERY;
import static org.folio.innreach.domain.entity.CentralServer.FETCH_ALL_BY_ID_QUERY_NAME;
import static org.folio.innreach.domain.entity.CentralServer.FETCH_CONNECTION_DETAILS_QUERY;
import static org.folio.innreach.domain.entity.CentralServer.FETCH_CONNECTION_DETAILS_QUERY_NAME;
import static org.folio.innreach.domain.entity.CentralServer.FETCH_ONE_BY_ID_QUERY;
import static org.folio.innreach.domain.entity.CentralServer.FETCH_ONE_BY_ID_QUERY_NAME;
import static org.folio.innreach.domain.entity.CentralServer.GET_IDS_QUERY;
import static org.folio.innreach.domain.entity.CentralServer.GET_IDS_QUERY_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.QueryHint;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.QueryHints;

import org.folio.innreach.domain.entity.base.Identifiable;

@Getter
@Setter
@EqualsAndHashCode(of = "localServerCode")
@ToString(exclude = {"centralServerCredentials", "localServerCredentials", "localAgencies"})
@Entity
@Table(name = "central_server")
@NamedQuery(
  name = FETCH_ALL_BY_ID_QUERY_NAME,
  query = FETCH_ALL_BY_ID_QUERY,
  hints = @QueryHint(name = QueryHints.PASS_DISTINCT_THROUGH, value = "false")
)
@NamedQuery(
  name = FETCH_ONE_BY_ID_QUERY_NAME,
  query = FETCH_ONE_BY_ID_QUERY,
  hints = @QueryHint(name = QueryHints.PASS_DISTINCT_THROUGH, value = "false")
)
@NamedQuery(
  name = GET_IDS_QUERY_NAME,
  query = GET_IDS_QUERY
)
@NamedQuery(
  name = FETCH_CONNECTION_DETAILS_QUERY_NAME,
  query = FETCH_CONNECTION_DETAILS_QUERY,
  hints = @QueryHint(name = QueryHints.PASS_DISTINCT_THROUGH, value = "false")
)
public class CentralServer implements Identifiable<UUID> {

  private static final String FETCH_BY_ID_POSTFIX = " WHERE cs.id = :id";

  private static final String FETCH_ALL_QUERY = "SELECT DISTINCT cs FROM CentralServer AS cs " +
    "LEFT JOIN FETCH cs.centralServerCredentials " +
    "LEFT JOIN FETCH cs.localServerCredentials " +
    "LEFT JOIN FETCH cs.localAgencies";

  public static final String FETCH_ALL_BY_ID_QUERY_NAME = "CentralServer.fetchAll";
  public static final String FETCH_ALL_BY_ID_QUERY = FETCH_ALL_QUERY + " WHERE cs.id IN :id";

  public static final String FETCH_ONE_BY_ID_QUERY_NAME = "CentralServer.fetchOne";
  public static final String FETCH_ONE_BY_ID_QUERY = FETCH_ALL_QUERY + FETCH_BY_ID_POSTFIX;

  public static final String GET_IDS_QUERY_NAME = "CentralServer.getIds";
  public static final String GET_IDS_QUERY = "SELECT DISTINCT cs.id FROM CentralServer AS cs";

  public static final String FETCH_CONNECTION_DETAILS_QUERY_NAME = "CentralServer.fetchConnectionDetails";
  public static final String FETCH_CONNECTION_DETAILS_QUERY =
    "SELECT new org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO(" +
      "cs.id, " +
      "cs.centralServerAddress, " +
      "cs.localServerCode, " +
      "cs.centralServerCredentials.centralServerKey, " +
      "cs.centralServerCredentials.centralServerSecret" +
    ") FROM CentralServer AS cs " + FETCH_BY_ID_POSTFIX;

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
  private UUID loanTypeId;

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
    cascade = CascadeType.ALL,
    fetch = FetchType.LAZY,
    mappedBy = "centralServer",
    orphanRemoval = true
  )
  private List<LocalAgency> localAgencies = new ArrayList<>();

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

  public void addLocalAgency(LocalAgency localAgency) {
    if (localAgency != null) {
      localAgency.setCentralServer(this);
    }
    this.localAgencies.add(localAgency);
  }

  public void removeLocalAgency(LocalAgency localAgency) {
    if (localAgency != null) {
      localAgency.setCentralServer(null);
    }
    this.localAgencies.remove(localAgency);
  }

}
