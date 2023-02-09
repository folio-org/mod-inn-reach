package org.folio.innreach.domain.entity;

import static org.folio.innreach.domain.entity.CentralServer.FETCH_ALL_BY_ID_QUERY;
import static org.folio.innreach.domain.entity.CentralServer.FETCH_ALL_BY_ID_QUERY_NAME;
import static org.folio.innreach.domain.entity.CentralServer.FETCH_CONNECTION_DETAILS_BY_CENTRAL_CODE_QUERY;
import static org.folio.innreach.domain.entity.CentralServer.FETCH_CONNECTION_DETAILS_BY_CENTRAL_CODE_QUERY_NAME;
import static org.folio.innreach.domain.entity.CentralServer.FETCH_CONNECTION_DETAILS_BY_ID_QUERY;
import static org.folio.innreach.domain.entity.CentralServer.FETCH_CONNECTION_DETAILS_BY_ID_QUERY_NAME;
import static org.folio.innreach.domain.entity.CentralServer.FETCH_ONE_BY_CENTRAL_CODE_QUERY;
import static org.folio.innreach.domain.entity.CentralServer.FETCH_ONE_BY_CENTRAL_CODE_QUERY_NAME;
import static org.folio.innreach.domain.entity.CentralServer.FETCH_ONE_BY_ID_QUERY;
import static org.folio.innreach.domain.entity.CentralServer.FETCH_ONE_BY_ID_QUERY_NAME;
import static org.folio.innreach.domain.entity.CentralServer.FETCH_RECALL_USER_BY_ID_QUERY;
import static org.folio.innreach.domain.entity.CentralServer.FETCH_RECALL_USER_BY_ID_QUERY_NAME;
import static org.folio.innreach.domain.entity.CentralServer.GET_IDS_QUERY;
import static org.folio.innreach.domain.entity.CentralServer.GET_IDS_QUERY_NAME;
import static org.folio.innreach.domain.entity.CentralServer.GET_ID_BY_CENTRAL_CODE_QUERY;
import static org.folio.innreach.domain.entity.CentralServer.GET_ID_BY_CENTRAL_CODE_QUERY_NAME;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

@Getter
@Setter
@EqualsAndHashCode(of = "localServerCode")
@ToString(exclude = {"centralServerCredentials", "localServerCredentials", "localAgencies"})
@Entity
@Table(name = "central_server")
@NamedQuery(
  name = FETCH_ALL_BY_ID_QUERY_NAME,
  query = FETCH_ALL_BY_ID_QUERY
)
@NamedQuery(
  name = FETCH_ONE_BY_ID_QUERY_NAME,
  query = FETCH_ONE_BY_ID_QUERY
)
@NamedQuery(
  name = FETCH_ONE_BY_CENTRAL_CODE_QUERY_NAME,
  query = FETCH_ONE_BY_CENTRAL_CODE_QUERY
)
@NamedQuery(
  name = GET_IDS_QUERY_NAME,
  query = GET_IDS_QUERY
)
@NamedQuery(
  name = GET_ID_BY_CENTRAL_CODE_QUERY_NAME,
  query = GET_ID_BY_CENTRAL_CODE_QUERY
)
@NamedQuery(
  name = FETCH_CONNECTION_DETAILS_BY_ID_QUERY_NAME,
  query = FETCH_CONNECTION_DETAILS_BY_ID_QUERY
)
@NamedQuery(
  name = FETCH_CONNECTION_DETAILS_BY_CENTRAL_CODE_QUERY_NAME,
  query = FETCH_CONNECTION_DETAILS_BY_CENTRAL_CODE_QUERY
)
@NamedQuery(
  name = FETCH_RECALL_USER_BY_ID_QUERY_NAME,
  query = FETCH_RECALL_USER_BY_ID_QUERY
)
public class CentralServer extends Auditable implements Identifiable<UUID> {

  private static final String FETCH_BY_ID_POSTFIX = " WHERE cs.id = :id";

  private static final String FETCH_BY_CENTRAL_CODE_POSTFIX = " WHERE cs.centralServerCode = :code";

  private static final String FETCH_ALL_QUERY = "SELECT DISTINCT cs FROM CentralServer AS cs " +
    "LEFT JOIN FETCH cs.centralServerCredentials " +
    "LEFT JOIN FETCH cs.localServerCredentials " +
    "LEFT JOIN FETCH cs.localAgencies";

  public static final String FETCH_ALL_BY_ID_QUERY_NAME = "CentralServer.fetchAll";
  public static final String FETCH_ALL_BY_ID_QUERY = FETCH_ALL_QUERY + " WHERE cs.id IN :id";

  public static final String FETCH_ONE_BY_ID_QUERY_NAME = "CentralServer.fetchOne";
  public static final String FETCH_ONE_BY_ID_QUERY = FETCH_ALL_QUERY + FETCH_BY_ID_POSTFIX;

  public static final String FETCH_ONE_BY_CENTRAL_CODE_QUERY_NAME = "CentralServer.fetchOneByCode";
  public static final String FETCH_ONE_BY_CENTRAL_CODE_QUERY = FETCH_ALL_QUERY + FETCH_BY_CENTRAL_CODE_POSTFIX;

  public static final String GET_IDS_QUERY_NAME = "CentralServer.getIds";
  public static final String GET_IDS_QUERY = "SELECT DISTINCT cs.id FROM CentralServer AS cs";

  public static final String GET_ID_BY_CENTRAL_CODE_QUERY_NAME = "CentralServer.getIdByCentralCode";
  public static final String GET_ID_BY_CENTRAL_CODE_QUERY = "SELECT cs.id FROM CentralServer cs WHERE cs.centralServerCode = :centralCode";

  public static final String FETCH_CONNECTION_DETAILS_BY_ID_QUERY_NAME = "CentralServer.fetchConnectionDetails";
  public static final String FETCH_CONNECTION_DETAILS_BY_CENTRAL_CODE_QUERY_NAME = "CentralServer.fetchConnectionDetailsByCode";

  public static final String FETCH_CONNECTION_DETAILS_QUERY =
    "SELECT new org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO(" +
      "cs.id, " +
      "cs.centralServerAddress, " +
      "cs.localServerCode, " +
      "cs.centralServerCode, " +
      "cs.centralServerCredentials.centralServerKey, " +
      "cs.centralServerCredentials.centralServerSecret" +
      ") FROM CentralServer AS cs ";

  public static final String FETCH_CONNECTION_DETAILS_BY_ID_QUERY = FETCH_CONNECTION_DETAILS_QUERY + FETCH_BY_ID_POSTFIX;
  public static final String FETCH_CONNECTION_DETAILS_BY_CENTRAL_CODE_QUERY = FETCH_CONNECTION_DETAILS_QUERY + FETCH_BY_CENTRAL_CODE_POSTFIX;

  public static final String FETCH_RECALL_USER_BY_ID_QUERY_NAME = "CentralServer.fetchRecallUser";
  public static final String FETCH_RECALL_USER_BY_ID_QUERY = "SELECT cs FROM CentralServer AS cs LEFT JOIN FETCH cs.innReachRecallUser " + FETCH_BY_ID_POSTFIX;

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;
  private String name;
  private String description;

  @Column(name = "check_pickup_location")
  private Boolean checkPickupLocation;

  @Column(name = "local_server_code")
  private String localServerCode;

  @Column(name = "central_server_code")
  private String centralServerCode;

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "inn_reach_recall_user_id")
  private InnReachRecallUser innReachRecallUser;

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
