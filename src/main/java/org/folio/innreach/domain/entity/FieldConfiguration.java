package org.folio.innreach.domain.entity;

import lombok.Getter;
import lombok.Setter;
import org.folio.innreach.domain.entity.base.Auditable;
import org.hibernate.annotations.Fetch;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "marc_field_configuration")
public class FieldConfiguration extends Auditable<String> {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "marc_transformation_options_settings_id")
  private MARCTransformationOptionsSettings MARCTransformationOptionsSettings;

  @Column(name = "resource_identifier_type_id")
  private UUID resourceIdentifierTypeId;

  @Column(name = "strip_prefix")
  private Boolean stripPrefix;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
    name = "marc_ignored_prefixes",
    joinColumns = @JoinColumn(name = "field_configuration_id")
  )
  @Column(name = "prefix")
  @Fetch(value = org.hibernate.annotations.FetchMode.SUBSELECT)
  private List<String> ignorePrefixes = new ArrayList<>();
}
