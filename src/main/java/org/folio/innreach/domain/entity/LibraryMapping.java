package org.folio.innreach.domain.entity;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.folio.innreach.domain.entity.base.Auditable;

@Getter
@Setter
@Entity
@Table(name = "library_mapping")
public class LibraryMapping extends Auditable<String> {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;
  private UUID libraryId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ir_location_id")
  private InnReachLocation innReachLocation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;
}
