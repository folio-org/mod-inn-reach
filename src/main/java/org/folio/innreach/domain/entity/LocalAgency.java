package org.folio.innreach.domain.entity;

import static org.folio.innreach.domain.entity.LocalAgency.FETCH_ONE_BY_CODE_QUERY;
import static org.folio.innreach.domain.entity.LocalAgency.FETCH_ONE_BY_CODE_QUERY_NAME;
import static org.folio.innreach.domain.entity.LocalAgency.FIND_LIBRARIES_ASSIGNED_TO_MULTIPLE_AGENCIES_QUERY;
import static org.folio.innreach.domain.entity.LocalAgency.FIND_LIBRARIES_ASSIGNED_TO_MULTIPLE_AGENCIES_QUERY_NAME;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Identifiable;

@Getter
@Setter
@EqualsAndHashCode(of = {"code"})
@ToString(exclude = {"folioLibraryIds", "centralServer"})
@Entity
@Table(name = "local_agency")
@NamedQuery(
  name = FETCH_ONE_BY_CODE_QUERY_NAME,
  query = FETCH_ONE_BY_CODE_QUERY
)
@NamedNativeQuery(
  name = FIND_LIBRARIES_ASSIGNED_TO_MULTIPLE_AGENCIES_QUERY_NAME,
  query = FIND_LIBRARIES_ASSIGNED_TO_MULTIPLE_AGENCIES_QUERY
)
public class LocalAgency implements Identifiable<UUID> {
  public static final String FETCH_ONE_BY_CODE_QUERY_NAME = "LocalAgency.fetchOneByCode";
  public static final String FETCH_ONE_BY_CODE_QUERY = "SELECT la FROM LocalAgency AS la WHERE la.code = :code";

  public static final String FIND_LIBRARIES_ASSIGNED_TO_MULTIPLE_AGENCIES_QUERY_NAME = "LocalAgency.findLibraryIdsAssignedToMultipleAgencies";
  public static final String FIND_LIBRARIES_ASSIGNED_TO_MULTIPLE_AGENCIES_QUERY = "SELECT Cast(folio_library_id as varchar) folio_library_id FROM local_agency la, " +
    "folio_library lib WHERE central_server_id = :centralServerId and lib.local_agency_id = la.id " +
    "group by folio_library_id HAVING COUNT(*) > 1";

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;
  private String code;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
    name = "folio_library",
    joinColumns = @JoinColumn(name = "local_agency_id")
  )
  @Column(name = "folio_library_id")
  @org.hibernate.annotations.Fetch(value = org.hibernate.annotations.FetchMode.SUBSELECT)
  private List<UUID> folioLibraryIds = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;
}
