package org.folio.innreach.domain.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "agencyCode")
@ToString(exclude = "localServerMapping")
@Entity
@Table(name = "agency_location_ac_mapping")
public class AgencyLocationAcMapping extends Auditable<String> implements Identifiable<UUID> {

  @GeneratedValue(strategy = GenerationType.AUTO)
  @Id
  private UUID id;

  private UUID locationId;

  private UUID libraryId;

  private String agencyCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "local_server_mapping_id", nullable = false, updatable = false)
  private AgencyLocationLscMapping localServerMapping;

}
