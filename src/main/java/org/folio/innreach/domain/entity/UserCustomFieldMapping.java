package org.folio.innreach.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

@Entity
@Getter
@Setter
@Table(name = "user_custom_field_mapping")
@ToString(exclude = {"centralServer"})
public class UserCustomFieldMapping extends Auditable implements Identifiable<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "custom_field_id")
  private String customFieldId;

  @ElementCollection(fetch = FetchType.LAZY)
  @JoinTable(name = "user_custom_field_configured_options", joinColumns = @JoinColumn(name = "user_custom_field_mapping_id"))
  @MapKeyColumn(name = "custom_field_value")
  @Column(name = "agency_code")
  @Fetch(value = FetchMode.JOIN)
  private Map<String, String> configuredOptions = new HashMap<>();

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;
}
