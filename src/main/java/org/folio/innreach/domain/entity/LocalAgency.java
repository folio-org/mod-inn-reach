package org.folio.innreach.domain.entity;

import static org.folio.innreach.domain.entity.LocalAgency.FETCH_ONE_BY_LIBRARY_ID;
import static org.folio.innreach.domain.entity.LocalAgency.FETCH_ONE_BY_LIBRARY_ID_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

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
  name = FETCH_ONE_BY_LIBRARY_ID_NAME,
  query = FETCH_ONE_BY_LIBRARY_ID
)
public class LocalAgency implements Identifiable<UUID> {
  public static final String FETCH_ONE_BY_LIBRARY_ID_NAME = "LocalAgency.fetchOneByLibraryId";
  public static final String FETCH_ONE_BY_LIBRARY_ID = "SELECT la FROM LocalAgency AS la " +
    "LEFT JOIN FETCH la.centralServer " +
    "WHERE :libraryId MEMBER OF la.folioLibraryIds";

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
