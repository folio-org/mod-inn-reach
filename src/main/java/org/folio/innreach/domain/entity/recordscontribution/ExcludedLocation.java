package org.folio.innreach.domain.entity.recordscontribution;

import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "excluded_locations")

@NoArgsConstructor
@Data
public class ExcludedLocation {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  UUID id;

  @Column(name = "location_id")
  UUID locationId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contribution_criteria_configuration_id")
  private ContributionCriteriaConfiguration centralServer;
}
