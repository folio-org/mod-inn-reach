package org.folio.innreach.domain.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Auditable;

@Entity
@Table(name = "item_contribution_options_configuration")
@Getter
@Setter
@ToString(exclude = { "notAvailableItemStatuses", "nonLendableLoanTypes", "nonLendableLocations", "nonLendableMaterialTypes"})
@EqualsAndHashCode(of = {"centralServerId"})
public class ItemContributionOptionsConfiguration extends Auditable<String> {

  @Id
  @Column(name = "central_server_id")
  private UUID centralServerId;

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
