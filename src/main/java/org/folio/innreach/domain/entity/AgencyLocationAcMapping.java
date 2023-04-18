package org.folio.innreach.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "agencyCode")
@ToString(exclude = "localServerMapping")
@Entity
@Table(name = "agency_location_ac_mapping")
public class AgencyLocationAcMapping extends Auditable implements Identifiable<UUID> {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private UUID locationId;

  private UUID libraryId;

  private String agencyCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "local_server_mapping_id", nullable = false, updatable = false)
  private AgencyLocationLscMapping localServerMapping;

}
