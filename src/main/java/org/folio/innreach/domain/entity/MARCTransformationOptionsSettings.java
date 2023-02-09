package org.folio.innreach.domain.entity;

import static org.folio.innreach.domain.entity.MARCTransformationOptionsSettings.FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY;
import static org.folio.innreach.domain.entity.MARCTransformationOptionsSettings.FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY_NAME;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;

import org.folio.innreach.domain.entity.base.Auditable;

@Entity
@Getter
@Setter
@Table(name = "marc_transformation_options_settings")
@NamedQuery(
  name = FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY_NAME,
  query = FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY
)
public class MARCTransformationOptionsSettings extends Auditable {
  public static final String FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY_NAME = "MARCTransformationOptionsSettings.fetchOne";
  public static final String FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY =
    "SELECT DISTINCT m FROM MARCTransformationOptionsSettings AS m where m.centralServer.id = :id";

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id", unique = true)
  private CentralServer centralServer;

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
    name = "marc_excluded_marc_fields",
    joinColumns = @JoinColumn(name = "marc_transformation_options_settings_id")
  )
  @Column(name = "marc_field")
  @Fetch(value = org.hibernate.annotations.FetchMode.SUBSELECT)
  private List<String> excludedMARCFields = new ArrayList<>();

  public void removeModifiedFieldForContributedRecords(FieldConfiguration fieldConfiguration) {
    if (fieldConfiguration != null) {
      fieldConfiguration.setMARCTransformationOptionsSettings(null);
    }
    this.modifiedFieldsForContributedRecords.remove(fieldConfiguration);
  }

  public void addModifiedFieldForContributedRecords(FieldConfiguration fieldConfiguration) {
    if (fieldConfiguration != null) {
      fieldConfiguration.setMARCTransformationOptionsSettings(this);
    }
    this.modifiedFieldsForContributedRecords.add(fieldConfiguration);
  }
}
