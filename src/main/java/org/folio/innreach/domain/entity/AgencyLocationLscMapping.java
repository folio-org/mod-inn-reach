package org.folio.innreach.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
import javax.persistence.Table;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "centralServerMapping")
@Entity
@Table(name = "agency_location_lsc_mapping")
public class AgencyLocationLscMapping implements Identifiable<UUID> {

  @GeneratedValue(strategy = GenerationType.AUTO)
  @Id
  private UUID id;

  private UUID locationId;

  private UUID libraryId;

  private String localServerCode;

  @OneToMany(
    cascade = CascadeType.ALL,
    fetch = FetchType.EAGER,
    orphanRemoval = true,
    mappedBy = "localServerMapping"
  )
  private List<AgencyLocationAcMapping> agencyCodeMappings;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_mapping_id", nullable = false)
  private AgencyLocationMapping centralServerMapping;

}
