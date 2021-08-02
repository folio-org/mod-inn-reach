package org.folio.innreach.domain.entity;

import static org.folio.innreach.domain.entity.MARCTransformationOptionsSettings.FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY;
import static org.folio.innreach.domain.entity.MARCTransformationOptionsSettings.FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY_NAME;

import lombok.Getter;
import lombok.Setter;
import org.folio.innreach.domain.entity.base.Auditable;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.QueryHints;

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
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "marc_transformation_options_settings")
@NamedQuery(
  name = FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY_NAME,
  query = FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY,
  hints = @QueryHint(name = QueryHints.PASS_DISTINCT_THROUGH, value = "false")
)
public class MARCTransformationOptionsSettings extends Auditable<String> {
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
