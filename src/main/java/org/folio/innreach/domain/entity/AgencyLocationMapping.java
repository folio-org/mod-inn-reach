package org.folio.innreach.domain.entity;

import static org.folio.innreach.domain.entity.AgencyLocationMapping.FETCH_ONE_BY_CS_QUERY;
import static org.folio.innreach.domain.entity.AgencyLocationMapping.FETCH_ONE_BY_CS_QUERY_NAME;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"centralServer", "localServerMappings"})
@Entity
@NamedQuery(
  name = FETCH_ONE_BY_CS_QUERY_NAME,
  query = FETCH_ONE_BY_CS_QUERY
)
@Table(name = "agency_location_mapping")
public class AgencyLocationMapping extends Auditable implements Identifiable<UUID> {

  private static final String FETCH_BY_CS_POSTFIX = " WHERE am.centralServer.id = :id";

  public static final String FETCH_ONE_BY_CS_QUERY_NAME = "AgencyLocationMapping.fetchOneByCs";

  public static final String FETCH_ONE_BY_CS_QUERY = "SELECT DISTINCT am FROM AgencyLocationMapping AS am " +
    "LEFT JOIN FETCH am.localServerMappings lsm " +
    "LEFT JOIN FETCH lsm.agencyCodeMappings" + FETCH_BY_CS_POSTFIX;

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private UUID locationId;

  private UUID libraryId;

  @OneToMany(
    cascade = CascadeType.ALL,
    fetch = FetchType.LAZY,
    orphanRemoval = true,
    mappedBy = "centralServerMapping"
  )
  @OrderBy("localServerCode")
  private Set<AgencyLocationLscMapping> localServerMappings = new LinkedHashSet<>();

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id", unique = true, nullable = false, updatable = false)
  private CentralServer centralServer;

  public void addLocalServerMapping(AgencyLocationLscMapping mapping) {
    Objects.requireNonNull(mapping);

    mapping.setCentralServerMapping(this);

    localServerMappings.add(mapping);
  }

  public void removeLocalServerMapping(AgencyLocationLscMapping mapping) {
    Objects.requireNonNull(mapping);

    localServerMappings.remove(mapping);
  }

}
