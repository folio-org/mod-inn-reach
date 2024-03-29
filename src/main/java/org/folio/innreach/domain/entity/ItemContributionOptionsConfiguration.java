package org.folio.innreach.domain.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

@Entity
@Table(name = "item_contribution_options_configuration")
@Getter
@Setter
@ToString(exclude = {"centralServer", "notAvailableItemStatuses", "nonLendableLoanTypes", "nonLendableLocations", "nonLendableMaterialTypes"})
public class ItemContributionOptionsConfiguration extends Auditable implements Identifiable<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id", unique = true)
  private CentralServer centralServer;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
    name = "not_available_item_status",
    joinColumns = @JoinColumn(name = "item_contribution_options_configuration_id")
  )
  @Column(name = "item_status")
  @org.hibernate.annotations.Fetch(value = org.hibernate.annotations.FetchMode.SUBSELECT)
  private List<String> notAvailableItemStatuses = new ArrayList<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
    name = "non_lendable_loan_type",
    joinColumns = @JoinColumn(name = "item_contribution_options_configuration_id")
  )
  @Column(name = "loan_type_id")
  @org.hibernate.annotations.Fetch(value = org.hibernate.annotations.FetchMode.SUBSELECT)
  private List<UUID> nonLendableLoanTypes = new ArrayList<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
    name = "non_lendable_location",
    joinColumns = @JoinColumn(name = "item_contribution_options_configuration_id")
  )
  @Column(name = "location_id")
  @org.hibernate.annotations.Fetch(value = org.hibernate.annotations.FetchMode.SUBSELECT)
  private List<UUID> nonLendableLocations = new ArrayList<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
    name = "non_lendable_material_type",
    joinColumns = @JoinColumn(name = "item_contribution_options_configuration_id")
  )
  @Column(name = "material_type_id")
  @org.hibernate.annotations.Fetch(value = org.hibernate.annotations.FetchMode.SUBSELECT)
  private List<UUID> nonLendableMaterialTypes = new ArrayList<>();
}
