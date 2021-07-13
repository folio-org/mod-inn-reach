package org.folio.innreach.domain.entity;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
public class LocationMapping extends AbstractEntity {

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
