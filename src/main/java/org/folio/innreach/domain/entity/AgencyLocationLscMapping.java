package org.folio.innreach.domain.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "localServerCode")
@ToString(exclude = {"centralServerMapping", "agencyCodeMappings"})
@Entity
@Table(name = "agency_location_lsc_mapping")
public class AgencyLocationLscMapping extends Auditable<String> implements Identifiable<UUID> {

  @GeneratedValue(strategy = GenerationType.AUTO)
  @Id
  private UUID id;

  private UUID locationId;

  private UUID libraryId;

  private String localServerCode;

  @OneToMany(
    cascade = CascadeType.ALL,
    fetch = FetchType.LAZY,
    orphanRemoval = true,
    mappedBy = "localServerMapping"
  )
  @OrderBy("agencyCode")
  private Set<AgencyLocationAcMapping> agencyCodeMappings;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_mapping_id", nullable = false, updatable = false)
  private AgencyLocationMapping centralServerMapping;

}
