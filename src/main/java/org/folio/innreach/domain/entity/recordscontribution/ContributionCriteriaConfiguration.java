package org.folio.innreach.domain.entity.recordscontribution;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.MetaData;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "contribution_criteria_configuration")

@NoArgsConstructor
@Data
public class ContributionCriteriaConfiguration {
  @Id
  UUID Id;

  @OneToMany(cascade = {CascadeType.ALL})
  @JoinColumn(name = "contribution_criteria_configuration_id")
  List<ExcludedLocation> excludedLocations;

  @OneToMany(cascade = {CascadeType.ALL})
  @JoinColumn(name = "contribution_criteria_configuration_id")
  List<StatisticalCodeBehavior> statisticalCodeBehaviors;

  @Embedded
  MetaData metaData;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;
}
