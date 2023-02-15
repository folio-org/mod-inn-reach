package org.folio.innreach.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.folio.innreach.domain.entity.base.AbstractEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "library_mapping")
public class LibraryMapping extends AbstractEntity {

  private UUID libraryId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ir_location_id")
  private InnReachLocation innReachLocation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;


  public LibraryMapping(UUID id) {
    super(id);
  }

}
