package org.folio.innreach.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "centralServer")
@Entity
@Table(name = "agency_location_mapping")
public class AgencyLocationMapping extends Auditable<String> implements Identifiable<UUID> {

  @Id
  @Column(name = "central_server_id")
  private UUID id;

  private UUID locationId;

  private UUID libraryId;

  @OneToMany(
    cascade = CascadeType.ALL,
    fetch = FetchType.EAGER,
    orphanRemoval = true,
    mappedBy = "centralServerMapping"
  )
  private List<AgencyLocationLscMapping> localServerMappings;

}
