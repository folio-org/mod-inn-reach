package org.folio.innreach.domain.entity;

import static org.folio.innreach.domain.entity.LocationMapping.FETCH_ALL_BY_CENTRAL_SERVER_QUERY;
import static org.folio.innreach.domain.entity.LocationMapping.FETCH_ALL_BY_CENTRAL_SERVER_QUERY_NAME;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.folio.innreach.domain.entity.base.AbstractEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "location_mapping")
@NamedQuery(
  name = FETCH_ALL_BY_CENTRAL_SERVER_QUERY_NAME,
  query = FETCH_ALL_BY_CENTRAL_SERVER_QUERY
)
public class LocationMapping extends AbstractEntity {

  public static final String FETCH_ALL_BY_CENTRAL_SERVER_QUERY_NAME = "LocationMapping.fetchAll";
  public static final String FETCH_ALL_BY_CENTRAL_SERVER_QUERY = "SELECT lm FROM LocationMapping AS lm " +
    "LEFT JOIN FETCH lm.innReachLocation WHERE lm.centralServer.id = : csId";

  private UUID locationId;
  private UUID libraryId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ir_location_id")
  private InnReachLocation innReachLocation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;

  public LocationMapping(UUID id) {
    super(id);
  }

}
