package org.folio.innreach.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
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
@EqualsAndHashCode(of = "localServerCode")
@ToString(exclude = {"centralServerMapping", "agencyCodeMappings"})
@Entity
@Table(name = "agency_location_lsc_mapping")
public class AgencyLocationLscMapping extends Auditable implements Identifiable<UUID> {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
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
  private Set<AgencyLocationAcMapping> agencyCodeMappings = new LinkedHashSet<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_mapping_id", nullable = false, updatable = false)
  private AgencyLocationMapping centralServerMapping;

  public void addAgencyCodeMapping(AgencyLocationAcMapping mapping) {
    Objects.requireNonNull(mapping);

    mapping.setLocalServerMapping(this);

    agencyCodeMappings.add(mapping);
  }

  public void removeAgencyCodeMapping(AgencyLocationAcMapping mapping) {
    Objects.requireNonNull(mapping);

    agencyCodeMappings.remove(mapping);
  }

}
