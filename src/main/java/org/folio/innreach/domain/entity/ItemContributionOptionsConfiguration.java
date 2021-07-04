package org.folio.innreach.domain.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "item_contribution_options_configuration")
@Getter
@Setter
@ToString(exclude = {"centralServer", "statuses", "loanTypes", "locations", "materialTypes"})
@EqualsAndHashCode(of = {"id"})
public class ItemContributionOptionsConfiguration {
  @Id
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
    name = "item_status",
    joinColumns = @JoinColumn(name = "item_contribution_options_configuration_id")
  )
  @Column(name = "item_status")
  @org.hibernate.annotations.Fetch(value = org.hibernate.annotations.FetchMode.SUBSELECT)
  private List<String> statuses = new ArrayList<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
    name = "loan_type",
    joinColumns = @JoinColumn(name = "item_contribution_options_configuration_id")
  )
  @Column(name = "loan_type_id")
  @org.hibernate.annotations.Fetch(value = org.hibernate.annotations.FetchMode.SUBSELECT)
  private List<UUID> loanTypes = new ArrayList<>();

  @ManyToMany(cascade = { CascadeType.ALL })
  @JoinTable(
    name = "location",
    joinColumns = { @JoinColumn(name = "item_contribution_options_configuration_id") },
    inverseJoinColumns = { @JoinColumn(name = "location_id") }
  )
  private List<InnReachLocation> locations = new ArrayList<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
    name = "material_type",
    joinColumns = @JoinColumn(name = "item_contribution_options_configuration_id")
  )
  @Column(name = "material_type_id")
  @org.hibernate.annotations.Fetch(value = org.hibernate.annotations.FetchMode.SUBSELECT)
  private List<UUID> materialTypes = new ArrayList<>();
}
