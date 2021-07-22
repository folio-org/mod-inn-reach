package org.folio.innreach.domain.entity;

import lombok.Getter;
import lombok.Setter;
import org.folio.innreach.domain.entity.base.Auditable;
import org.hibernate.annotations.Fetch;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "marc_transformation_options_settings")
public class MARCTransformationOptionsSettings extends Auditable<String> {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id", unique = true)
  private CentralServer centralServer;

  @Column(name = "central_server_record_id")
  private UUID centralServerRecordId;

  @Column(name = "config_is_active")
  private Boolean configIsActive;

  @OneToMany(
    cascade = CascadeType.ALL,
    fetch = FetchType.LAZY,
    mappedBy = "MARCTransformationOptionsSettings",
    orphanRemoval = true
  )
  private List<FieldConfiguration> modifiedFieldsForContributedRecords = new ArrayList<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
    name = "excluded_marc_fields",
    joinColumns = @JoinColumn(name = "marc_transformation_options_settings_id")
  )
  @Column(name = "marc_field")
  @Fetch(value = org.hibernate.annotations.FetchMode.SUBSELECT)
  private List<String> excludedMARCFields = new ArrayList<>();
}
