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
import jakarta.persistence.ManyToOne;
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
@Table(name = "marc_field_configuration")
public class FieldConfiguration extends Auditable {
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
