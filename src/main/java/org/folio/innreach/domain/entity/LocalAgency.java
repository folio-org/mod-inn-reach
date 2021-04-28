package org.folio.innreach.domain.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(of = {"code"})
@ToString(exclude = {"folioLibrariesIds", "centralServer"})
@Entity
@Table(name = "local_agency")
public class LocalAgency {

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
  private Set<String> folioLibrariesIds = new HashSet<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;

  public void addFolioLibraryId(String folioLibraryId) {
    folioLibrariesIds.add(folioLibraryId);
  }

  public void removeFolioLibraryId(String folioLibraryId) {
    folioLibrariesIds.remove(folioLibraryId);
  }
}
