package org.folio.innreach.domain.entity;

import static org.folio.innreach.domain.entity.LocalAgency.FETCH_ONE_BY_CODE_QUERY;
import static org.folio.innreach.domain.entity.LocalAgency.FETCH_ONE_BY_CODE_QUERY_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Identifiable;

@Getter
@Setter
@EqualsAndHashCode(of = {"code"})
@ToString(exclude = {"folioLibraries", "centralServer"})
@Entity
@Table(name = "local_agency")
@NamedQuery(
  name = FETCH_ONE_BY_CODE_QUERY_NAME,
  query = FETCH_ONE_BY_CODE_QUERY
)
public class LocalAgency implements Identifiable<UUID> {
  public static final String FETCH_ONE_BY_CODE_QUERY_NAME = "LocalAgency.fetchOneByCode";
  public static final String FETCH_ONE_BY_CODE_QUERY = "SELECT la FROM LocalAgency AS la WHERE la.code = :code";

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;
  private String code;

  @ElementCollection(targetClass = FolioLibrary.class, fetch = FetchType.LAZY)
  @CollectionTable(
    name = "folio_library",
    joinColumns = @JoinColumn(name = "local_agency_id")
  )
  @Column(name = "folio_library_id")
  @org.hibernate.annotations.Fetch(value = org.hibernate.annotations.FetchMode.SUBSELECT)
  private List<FolioLibrary> folioLibraries = new ArrayList<>();


  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;

  @Data
  @AllArgsConstructor
  @Embeddable
  @NoArgsConstructor
  public static class FolioLibrary {
    @Column(name = "folio_library_id")
    private UUID folioLibraryId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "central_server_id")
    private CentralServer centralServer;
  }
}
